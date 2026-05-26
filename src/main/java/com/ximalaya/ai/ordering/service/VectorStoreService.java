package com.ximalaya.ai.ordering.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ximalaya.ai.ordering.config.RagProperties;
import com.ximalaya.ai.ordering.entity.DishEmbedding;
import com.ximalaya.ai.ordering.repository.DishEmbeddingRepository;
import com.ximalaya.ai.ordering.vector.ScoredDocument;
import com.ximalaya.ai.ordering.vector.VectorMath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class VectorStoreService {

    private static final Logger log = LoggerFactory.getLogger(VectorStoreService.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final DishEmbeddingRepository embeddingRepository;
    private final RagProperties ragProperties;

    private final ConcurrentHashMap<Long, IndexedVector> memoryIndex = new ConcurrentHashMap<>();

    public VectorStoreService(DishEmbeddingRepository embeddingRepository, RagProperties ragProperties) {
        this.embeddingRepository = embeddingRepository;
        this.ragProperties = ragProperties;
    }

    public Mono<Void> upsert(Long dishId, String contentText, String contentHash,
                             float[] embedding, String dishName, String category) {
        return Mono.fromCallable(() -> MAPPER.writeValueAsString(toDoubleList(embedding)))
                .flatMap(json -> {
                    DishEmbedding row = new DishEmbedding();
                    row.setDishId(dishId);
                    row.setContentText(contentText);
                    row.setContentHash(contentHash);
                    row.setEmbeddingJson(json);
                    row.setDimension(embedding.length);
                    row.setUpdatedAt(LocalDateTime.now());
                    return embeddingRepository.save(row);
                })
                .doOnSuccess(saved -> memoryIndex.put(dishId, new IndexedVector(
                        dishId, embedding, contentText, dishName, category)))
                .then();
    }

    public Mono<Void> remove(Long dishId) {
        memoryIndex.remove(dishId);
        return embeddingRepository.deleteById(dishId).then();
    }

    public Mono<Void> reloadFromDatabase() {
        return embeddingRepository.findAll()
                .doOnNext(row -> {
                    try {
                        float[] vector = fromJson(row.getEmbeddingJson());
                        String[] meta = parseMeta(row.getContentText());
                        memoryIndex.put(row.getDishId(), new IndexedVector(
                                row.getDishId(), vector, row.getContentText(), meta[0], meta[1]));
                    } catch (Exception e) {
                        log.warn("加载向量失败 dishId={}: {}", row.getDishId(), e.getMessage());
                    }
                })
                .then()
                .doOnSuccess(v -> log.info("向量索引已加载: {} 条", memoryIndex.size()));
    }

    public Mono<List<ScoredDocument>> similaritySearch(float[] queryVector, int topK, double minScore) {
        return Mono.fromCallable(() -> {
            List<ScoredDocument> results = new ArrayList<>();
            for (IndexedVector doc : memoryIndex.values()) {
                double score = VectorMath.cosineSimilarity(queryVector, doc.embedding());
                if (score >= minScore) {
                    results.add(new ScoredDocument(
                            doc.dishId(), doc.contentText(), score, doc.dishName(), doc.category()));
                }
            }
            results.sort(Comparator.comparingDouble(ScoredDocument::score).reversed());
            int limit = Math.min(topK, results.size());
            return results.subList(0, limit);
        });
    }

    public Mono<Long> count() {
        return Mono.just((long) memoryIndex.size());
    }

    private static List<Double> toDoubleList(float[] vector) {
        List<Double> list = new ArrayList<>(vector.length);
        for (float v : vector) {
            list.add((double) v);
        }
        return list;
    }

    private static float[] fromJson(String json) throws Exception {
        List<Double> values = MAPPER.readValue(json, new TypeReference<>() {});
        float[] vector = new float[values.size()];
        for (int i = 0; i < values.size(); i++) {
            vector[i] = values.get(i).floatValue();
        }
        return vector;
    }

    /** contentText 首行存菜名、分类，便于检索结果展示 */
    static String buildContentText(String dishName, String category, String description, String price) {
        return "菜名:" + dishName + "\n分类:" + category + "\n描述:" + description + "\n价格:" + price;
    }

    private static String[] parseMeta(String contentText) {
        String name = "";
        String category = "";
        for (String line : contentText.split("\n")) {
            if (line.startsWith("菜名:")) {
                name = line.substring(3).trim();
            } else if (line.startsWith("分类:")) {
                category = line.substring(3).trim();
            }
        }
        return new String[]{name, category};
    }

    int indexSize() {
        return memoryIndex.size();
    }

    private record IndexedVector(Long dishId, float[] embedding, String contentText,
                                 String dishName, String category) {
    }
}
