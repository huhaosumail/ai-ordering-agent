package com.ximalaya.ai.ordering.evaluation.model;

import java.time.Instant;

public record EvalReport(
        Instant ranAt,
        EvalSuiteResult rag,
        EvalSuiteResult agentIntent,
        boolean allPassed
) {
    public static EvalReport of(EvalSuiteResult rag, EvalSuiteResult agentIntent) {
        boolean all = rag.passRate() >= 1.0 && agentIntent.passRate() >= 1.0;
        return new EvalReport(Instant.now(), rag, agentIntent, all);
    }
}
