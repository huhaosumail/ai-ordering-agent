package com.ximalaya.ai.ordering.service;

import com.ximalaya.ai.ordering.config.EmbeddingProperties;
import com.ximalaya.ai.ordering.embedding.DoubaoArkEmbeddingClient;
import com.ximalaya.ai.ordering.embedding.EmbeddingClient;
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
        assertArkProvider(properties);
        OkHttpClient httpClient = new OkHttpClient.Builder()
                .connectTimeout(Duration.ofSeconds(15))
                .readTimeout(Duration.ofSeconds(60))
                .writeTimeout(Duration.ofSeconds(15))
                .build();
        this.embeddingClient = new DoubaoArkEmbeddingClient(properties, httpClient);
        if (properties.isConfigured()) {
            log.info("Embedding 火山方舟已配置 endpoint={}, baseUrl={}",
                    properties.getModel(), properties.getBaseUrl());
        } else {
            log.warn("Embedding 未配置 ARK_API_KEY / AI_EMBEDDING_MODEL(ep-xxx)，"
                    + "RAG 将{}",
                    properties.isFallbackLocal() ? "使用本地向量兜底" : "在调用时失败");
        }
    }

    private static void assertArkProvider(EmbeddingProperties properties) {
        String provider = properties.getProvider() == null ? "" : properties.getProvider().trim().toLowerCase();
        if (!provider.isEmpty()
                && !"doubao-ark".equals(provider)
                && !"ark".equals(provider)
                && !"volcengine-ark".equals(provider)) {
            throw new IllegalStateException(
                    "仅支持火山方舟 Embedding（ai.embedding.provider=doubao-ark），当前: " + provider);
        }
        properties.setProvider("doubao-ark");
    }

    public EmbeddingProperties getProperties() {
        return properties;
    }

    public Mono<float[]> embed(String text) {
        if (text == null || text.isBlank()) {
            return Mono.just(localEmbed(""));
        }
        if (!properties.isConfigured()) {
            if (properties.isFallbackLocal()) {
                return Mono.just(localEmbed(text));
            }
            return Mono.error(new IllegalStateException(
                    "未配置火山方舟：请设置 ARK_API_KEY 与 AI_EMBEDDING_MODEL=ep-xxx"));
        }
        return Mono.fromCallable(() -> embeddingClient.embed(text))
                .onErrorResume(e -> {
                    log.warn("火山方舟 Embedding 失败，{}: {}",
                            properties.isFallbackLocal() ? "使用本地向量" : "抛出异常",
                            e.getMessage());
                    if (properties.isFallbackLocal()) {
                        return Mono.just(localEmbed(text));
                    }
                    return Mono.error(e);
                });
    }

    /**
     * 本地确定性向量（仅 fallback-local=true 且 API 不可用时）
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
