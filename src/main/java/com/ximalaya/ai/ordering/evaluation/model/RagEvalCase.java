package com.ximalaya.ai.ordering.evaluation.model;

import java.util.List;

public record RagEvalCase(
        String id,
        String query,
        List<String> expectedDishes,
        int topK,
        double minRecall
) {
    public int effectiveTopK() {
        return topK > 0 ? topK : 5;
    }

    public double effectiveMinRecall() {
        return minRecall > 0 ? minRecall : 0.5;
    }
}
