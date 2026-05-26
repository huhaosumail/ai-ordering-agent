package com.ximalaya.ai.ordering.embedding;

import com.ximalaya.ai.ordering.config.EmbeddingProperties;

/**
 * 火山方舟 OpenAI 兼容向量化：POST {baseUrl}/embeddings，model 为控制台接入点 ep-xxx。
 * 文档：https://www.volcengine.com/docs/82379
 */
public class DoubaoArkEmbeddingClient extends OpenAiCompatibleEmbeddingClient {

    public DoubaoArkEmbeddingClient(EmbeddingProperties properties, okhttp3.OkHttpClient httpClient) {
        super(properties, httpClient);
    }
}
