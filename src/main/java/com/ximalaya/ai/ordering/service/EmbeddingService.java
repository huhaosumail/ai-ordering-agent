package com.ximalaya.ai.ordering.service;

import com.ximalaya.ai.ordering.config.EmbeddingProperties;
import com.ximalaya.ai.ordering.embedding.DoubaoArkEmbeddingClient;
import com.ximalaya.ai.ordering.embedding.DoubaoBgeM3EmbeddingClient;
import com.ximalaya.ai.ordering.embedding.EmbeddingClient;
import com.ximalaya.ai.ordering.embedding.OpenAiCompatibleEmbeddingClient;
import com.ximalaya.ai.ordering.vector.VectorMath;
import okhttp3.OkHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;

@Service
public class EmbeddingService {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingService.class);

    private final EmbeddingProperties properties;
    private final EmbeddingClient embeddingClient;

    public EmbeddingService(EmbeddingProperties properties) {
        this.properties = properties;
        OkHttpClient httpClient = new OkHttpClient.Builder()
                .connectTimeout(Duration.ofSeconds(15))
                .readTimeout(Duration.ofSeconds(60))
                .writeTimeout(Duration.ofSeconds(15))
                .build();
        this.embeddingClient = createClient(properties, httpClient);
        log.info("Embedding 已启用 provider={}, model/bge={}",
                properties.getProvider(),
                "doubao-bge-m3".equalsIgnoreCase(properties.getProvider())
                        ? properties.getModelName() : properties.getModel());
    }

    private static EmbeddingClient createClient(EmbeddingProperties properties, OkHttpClient httpClient) {
        String provider = properties.getProvider() == null ? "openai" : properties.getProvider().trim().toLowerCase();
        return switch (provider) {
            case "doubao-ark", "ark", "volcengine-ark" -> new DoubaoArkEmbeddingClient(properties, httpClient);
            case "doubao-bge-m3", "bge-m3", "vikingdb-bge-m3" -> new DoubaoBgeM3EmbeddingClient(properties, httpClient);
            default -> new OpenAiCompatibleEmbeddingClient(properties, httpClient);
        };
    }

    public Mono<float[]> embed(String text) {
        if (text == null || text.isBlank()) {
            return Mono.just(localEmbed(""));
        }
        return Mono.fromCallable(() -> embeddingClient.embed(text))
                .onErrorResume(e -> {
                    log.warn("Embedding [{}] 失败，{}: {}",
                            properties.getProvider(),
                            properties.isFallbackLocal() ? "使用本地向量" : "抛出异常",
                            e.getMessage());
                    if (properties.isFallbackLocal()) {
                        return Mono.just(localEmbed(text));
                    }
                    return Mono.error(e);
                });
    }

    /**
     * 本地确定性向量（API 不可用时的兜底，维度由 ai.embedding.dimensions 控制）
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
