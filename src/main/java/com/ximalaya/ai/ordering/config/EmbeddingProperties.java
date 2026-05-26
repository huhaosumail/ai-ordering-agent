package com.ximalaya.ai.ordering.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "ai.embedding")
public class EmbeddingProperties {

    /** 固定为火山方舟 doubao-ark */
    private String provider = "doubao-ark";

    private String apiKey = "";
    private String baseUrl = "https://ark.cn-beijing.volces.com/api/v3";
    /** 方舟控制台创建的 Embedding 推理接入点 ID，如 ep-xxx */
    private String model = "";

    /**
     * true：调用 /embeddings/multimodal（doubao-embedding-vision 等接入点）
     * false：调用 /embeddings（纯文本 Embedding 接入点）
     */
    private boolean multimodal = false;

    /** 本地 fallback 向量维度（仅 fallback-local=true 时使用） */
    private int dimensions = 1024;
    private boolean fallbackLocal = false;

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

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

    public boolean isMultimodal() {
        return multimodal;
    }

    public void setMultimodal(boolean multimodal) {
        this.multimodal = multimodal;
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

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank()
                && model != null && !model.isBlank();
    }
}
