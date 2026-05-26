package com.ximalaya.ai.ordering.embedding;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DoubaoBgeM3EmbeddingClientTest {

    @Test
    void parseBgeM3Response_extracts1024DimVector() throws Exception {
        String json = """
                {
                  "code": 0,
                  "message": "success",
                  "data": {
                    "sentence_dense_embedding": [[0.1, 0.2, 0.3]]
                  }
                }
                """;
        float[] vector = DoubaoBgeM3EmbeddingClient.parseBgeM3Response(json);
        assertEquals(3, vector.length);
        assertTrue(vector[0] > 0);
    }

    @Test
    void buildAuthorization_addsTokenPrefix() {
        assertEquals("token=abc", DoubaoBgeM3EmbeddingClient.buildAuthorization("abc"));
        assertEquals("Bearer xyz", DoubaoBgeM3EmbeddingClient.buildAuthorization("Bearer xyz"));
    }
}
