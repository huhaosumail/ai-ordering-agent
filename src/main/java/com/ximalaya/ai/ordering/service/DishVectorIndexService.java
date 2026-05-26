package com.ximalaya.ai.ordering.service;

import com.ximalaya.ai.ordering.entity.Dish;
import com.ximalaya.ai.ordering.repository.DishEmbeddingRepository;
import com.ximalaya.ai.ordering.repository.DishRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class DishVectorIndexService {

    private static final Logger log = LoggerFactory.getLogger(DishVectorIndexService.class);

    private final DishRepository dishRepository;
    private final DishEmbeddingRepository embeddingRepository;
    private final EmbeddingService embeddingService;
    private final VectorStoreService vectorStoreService;

    public DishVectorIndexService(DishRepository dishRepository,
                                  DishEmbeddingRepository embeddingRepository,
                                  EmbeddingService embeddingService,
                                  VectorStoreService vectorStoreService) {
        this.dishRepository = dishRepository;
        this.embeddingRepository = embeddingRepository;
        this.embeddingService = embeddingService;
        this.vectorStoreService = vectorStoreService;
    }

    public Mono<Void> reindexAll() {
        log.info("开始全量重建菜品向量索引...");
        return dishRepository.findByIsAvailableTrue()
                .flatMap(this::indexDish)
                .then(vectorStoreService.reloadFromDatabase())
                .doOnSuccess(v -> log.info("菜品向量索引重建完成, size={}", vectorStoreService.indexSize()));
    }

    public Mono<Void> indexDish(Dish dish) {
        if (dish.getId() == null) {
            return Mono.empty();
        }
        String content = buildIndexText(dish);
        String hash = embeddingService.contentHash(content);
        return embeddingRepository.findById(dish.getId())
                .flatMap(existing -> {
                    if (hash.equals(existing.getContentHash())) {
                        log.debug("菜品向量未变化，跳过: id={}, name={}", dish.getId(), dish.getName());
                        return Mono.empty();
                    }
                    return embedAndSave(dish, content, hash);
                })
                .switchIfEmpty(embedAndSave(dish, content, hash));
    }

    public Mono<Void> removeDishIndex(Long dishId) {
        return vectorStoreService.remove(dishId);
    }

    private Mono<Void> embedAndSave(Dish dish, String content, String hash) {
        return embeddingService.embed(content)
                .flatMap(vector -> vectorStoreService.upsert(
                        dish.getId(),
                        content,
                        hash,
                        vector,
                        dish.getName(),
                        dish.getCategory() != null ? dish.getCategory() : ""))
                .doOnSuccess(v -> log.debug("已索引菜品: id={}, name={}", dish.getId(), dish.getName()));
    }

    private static String buildIndexText(Dish dish) {
        String desc = dish.getDescription() != null ? dish.getDescription() : "";
        String price = dish.getPrice() != null ? dish.getPrice().toPlainString() + "元" : "";
        String category = dish.getCategory() != null ? dish.getCategory() : "";
        return VectorStoreService.buildContentText(dish.getName(), category, desc, price);
    }
}
