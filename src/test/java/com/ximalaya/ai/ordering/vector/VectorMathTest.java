package com.ximalaya.ai.ordering.vector;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class VectorMathTest {

    @Test
    void cosineSimilarity_sameVectorIsOne() {
        float[] v = {1f, 0f, 1f};
        assertTrue(VectorMath.cosineSimilarity(v, v) > 0.99);
    }

    @Test
    void cosineSimilarity_orthogonalIsZero() {
        float[] a = {1f, 0f};
        float[] b = {0f, 1f};
        assertTrue(VectorMath.cosineSimilarity(a, b) < 0.01);
    }
}
