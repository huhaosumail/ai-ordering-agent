package com.ximalaya.ai.ordering.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ximalaya.ai.ordering.config.EmbeddingProperties;
import com.ximalaya.ai.ordering.vector.VectorMath;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class EmbeddingService {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingService.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private final EmbeddingProperties properties;
    private final OkHttpClient httpClient;

    public EmbeddingService(EmbeddingProperties properties) {
        this.properties = properties;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(Duration.ofSeconds(15))
                .readTimeout(Duration.ofSeconds(30))
                .build();
    }

    public Mono<float[]> embed(String text) {
        if (text == null || text.isBlank()) {
            return Mono.just(localEmbed(""));
        }
        return Mono.fromCallable(() -> callEmbeddingApi(text))
                .onErrorResume(e -> {
                    log.warn("Embedding API 失败，使用本地向量: {}", e.getMessage());
                    if (properties.isFallbackLocal()) {
                        return Mono.just(localEmbed(text));
                    }
                    return Mono.error(e);
                })
                .switchIfEmpty(Mono.fromCallable(() -> localEmbed(text)));
    }

    private float[] callEmbeddingApi(String text) throws Exception {
        if (properties.getApiKey() == null || properties.getApiKey().isBlank()
                || properties.getApiKey().startsWith("your-")) {
            if (properties.isFallbackLocal()) {
                return localEmbed(text);
            }
            throw new IllegalStateException("未配置有效的 embedding API Key");
        }

        Map<String, Object> body = new HashMap<>();
        body.put("model", properties.getModel());
        body.put("input", text);

        Request request = new Request.Builder()
                .url(properties.getBaseUrl() + "/embeddings")
                .header("Authorization", "Bearer " + properties.getApiKey())
                .header("Content-Type", "application/json")
                .post(RequestBody.create(MAPPER.writeValueAsString(body), JSON))
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IllegalStateException("HTTP " + response.code());
            }
            String resp = response.body() != null ? response.body().string() : "";
            List<Double> values = parseEmbeddingResponse(resp);
            if (values.isEmpty()) {
                throw new IllegalStateException("empty embedding");
            }
            float[] vector = new float[values.size()];
            for (int i = 0; i < values.size(); i++) {
                vector[i] = values.get(i).floatValue();
            }
            return VectorMath.l2Normalize(vector);
        }
    }

    @SuppressWarnings("unchecked")
    private List<Double> parseEmbeddingResponse(String json) throws Exception {
        Map<String, Object> root = MAPPER.readValue(json, new TypeReference<>() {});
        List<Map<String, Object>> data = (List<Map<String, Object>>) root.get("data");
        if (data == null || data.isEmpty()) {
            return List.of();
        }
        return (List<Double>) data.get(0).get("embedding");
    }

    /**
     * 本地确定性向量：字符 n-gram 哈希桶 + L2 归一化，用于 API 不可用时的语义近似检索
     */
    public float[] localEmbed(String text) {
        int dim = properties.getDimensions();
        float[] vector = new float[dim];
        String normalized = text.toLowerCase().trim();
        if (normalized.isEmpty()) {
            return vector;
        }
        for (int i = 0; i < normalized.length(); i++) {
            char c = normalized.charAt(i);
            int bucket = Math.floorMod(c * 31 + i, dim);
            vector[bucket] += 1.0f;
        }
        for (int i = 0; i < normalized.length() - 1; i++) {
            String bi = normalized.substring(i, i + 2);
            int bucket = Math.floorMod(bi.hashCode(), dim);
            vector[bucket] += 1.5f;
        }
        return VectorMath.l2Normalize(vector);
    }

    public String contentHash(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(content.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return String.valueOf(content.hashCode());
        }
    }
}
