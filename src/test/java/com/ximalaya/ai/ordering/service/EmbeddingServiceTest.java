package com.ximalaya.ai.ordering.service;

import com.ximalaya.ai.ordering.config.EmbeddingProperties;
import com.ximalaya.ai.ordering.vector.VectorMath;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class EmbeddingServiceTest {

    @Test
    void localEmbed_similarTextsHaveHigherSimilarity() {
        EmbeddingProperties props = new EmbeddingProperties();
        props.setDimensions(256);
        EmbeddingService service = new EmbeddingService(props);

        float[] spicy = service.localEmbed("麻辣鲜香 下饭");
        float[] mapo = service.localEmbed("麻婆豆腐 麻辣");
        float[] dessert = service.localEmbed("提拉米苏 甜点");

        double spicyMapo = VectorMath.cosineSimilarity(spicy, mapo);
        double spicyDessert = VectorMath.cosineSimilarity(spicy, dessert);
        assertTrue(spicyMapo > spicyDessert);
    }
}
