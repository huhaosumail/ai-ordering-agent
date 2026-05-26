package com.ximalaya.ai.ordering.embedding;

import com.ximalaya.ai.ordering.config.EmbeddingProperties;

/**
 * 火山方舟 OpenAI 兼容向量化（需在控制台创建 embedding 推理接入点，model 填 ep-xxx）
 * 文档：https://www.volcengine.com/docs/82379
 */
public class DoubaoArkEmbeddingClient extends OpenAiCompatibleEmbeddingClient {

    public DoubaoArkEmbeddingClient(EmbeddingProperties properties, okhttp3.OkHttpClient httpClient) {
        super(properties, httpClient);
    }
}
