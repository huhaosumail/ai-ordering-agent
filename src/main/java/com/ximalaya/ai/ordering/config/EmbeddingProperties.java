package com.ximalaya.ai.ordering.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "ai.embedding")
public class EmbeddingProperties {

    private String apiKey = "";
    private String baseUrl = "https://api.deepseek.com/v1";
    private String model = "text-embedding-ada-002";
    private int dimensions = 256;
    /**
     * API 失败时是否使用本地哈希向量（保证无 Key 时 RAG 仍可演示）
     */
    private boolean fallbackLocal = true;

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public int getDimensions() {
        return dimensions;
    }

    public void setDimensions(int dimensions) {
        this.dimensions = dimensions;
    }

    public boolean isFallbackLocal() {
        return fallbackLocal;
    }

    public void setFallbackLocal(boolean fallbackLocal) {
        this.fallbackLocal = fallbackLocal;
    }
}
