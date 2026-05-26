package com.ximalaya.ai.ordering.embedding;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DoubaoArkEmbeddingClientTest {

    @Test
    void parseMultimodalEmbedding_readsDataEmbeddingArray() throws Exception {
        String json = """
                {"data":{"embedding":[0.1,0.2,0.3]}}
                """;
        float[] vector = DoubaoArkEmbeddingClient.parseMultimodalEmbedding(json);
        assertEquals(3, vector.length);
        assertEquals(0.1f, vector[0], 1e-5f);
    }
}
