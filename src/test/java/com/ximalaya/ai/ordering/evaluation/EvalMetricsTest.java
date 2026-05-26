package com.ximalaya.ai.ordering.evaluation;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EvalMetricsTest {

    @Test
    void hitAtK_and_recall() {
        List<String> retrieved = List.of("麻婆豆腐", "鱼香肉丝", "宫保鸡丁");
        List<String> expected = List.of("麻婆豆腐", "提拉米苏");
        assertTrue(EvalMetrics.hitAtK(retrieved, expected, 3));
        assertEquals(0.5, EvalMetrics.recallAtK(retrieved, expected, 3), 1e-6);
        assertEquals(1.0, EvalMetrics.reciprocalRank(retrieved, expected), 1e-6);
    }
}
