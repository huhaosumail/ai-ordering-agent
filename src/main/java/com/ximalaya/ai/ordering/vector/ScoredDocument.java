package com.ximalaya.ai.ordering.vector;

public record ScoredDocument(
        Long dishId,
        String content,
        double score,
        String dishName,
        String category
) {
}
