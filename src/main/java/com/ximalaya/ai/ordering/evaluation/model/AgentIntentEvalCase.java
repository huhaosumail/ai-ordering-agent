package com.ximalaya.ai.ordering.evaluation.model;

public record AgentIntentEvalCase(
        String id,
        String message,
        String expectedTool
) {
}
