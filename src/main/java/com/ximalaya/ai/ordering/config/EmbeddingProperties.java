package com.ximalaya.ai.ordering.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "ai.embedding")
public class EmbeddingProperties {

    /**
     * openai：OpenAI 兼容 /embeddings
     * doubao-ark：火山方舟 https://ark.cn-beijing.volces.com/api/v3/embeddings
     * doubao-bge-m3：VikingDB embedding v2，model_name=bge-m3（1024 维）
     */
    private String provider = "doubao-bge-m3";

    private String apiKey = "";
    private String baseUrl = "https://api.deepseek.com/v1";
    /** OpenAI 兼容或方舟接入点 ID / 模型名 */
    private String model = "text-embedding-ada-002";

    /** VikingDB bge-m3：服务 Host */
    private String host = "https://api-vikingdb.vikingdb.cn-beijing.volces.com";
    /** VikingDB embedding v2 的 model_name */
    private String modelName = "bge-m3";
    private int dimensions = 1024;
    private int embeddingDimension = 0;
    private boolean returnDense = true;
    private boolean returnSparse = false;
    private boolean fallbackLocal = true;

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

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public int getDimensions() {
        return dimensions;
    }

    public void setDimensions(int dimensions) {
        this.dimensions = dimensions;
    }

    public int getEmbeddingDimension() {
        return embeddingDimension;
    }

    public void setEmbeddingDimension(int embeddingDimension) {
        this.embeddingDimension = embeddingDimension;
    }

    public boolean isReturnDense() {
        return returnDense;
    }

    public void setReturnDense(boolean returnDense) {
        this.returnDense = returnDense;
    }

    public boolean isReturnSparse() {
        return returnSparse;
    }

    public void setReturnSparse(boolean returnSparse) {
        this.returnSparse = returnSparse;
    }

    public boolean isFallbackLocal() {
        return fallbackLocal;
    }

    public void setFallbackLocal(boolean fallbackLocal) {
        this.fallbackLocal = fallbackLocal;
    }
}
