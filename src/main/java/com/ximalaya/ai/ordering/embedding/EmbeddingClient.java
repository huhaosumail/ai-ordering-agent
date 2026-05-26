package com.ximalaya.ai.ordering.embedding;

/**
 * 文本向量化客户端（OpenAI 兼容 / 火山方舟 / VikingDB bge-m3）
 */
public interface EmbeddingClient {

    float[] embed(String text) throws Exception;
}
