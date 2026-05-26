package com.ximalaya.ai.ordering.evaluation.model;

import java.util.List;

public record EvalSuiteResult(
        String suite,
        int total,
        int passed,
        double passRate,
        double aggregateScore,
        List<EvalCaseResult> cases
) {
    public static EvalSuiteResult of(String suite, List<EvalCaseResult> cases, double aggregateScore) {
        int passed = (int) cases.stream().filter(EvalCaseResult::passed).count();
        int total = cases.size();
        double passRate = total == 0 ? 1.0 : (double) passed / total;
        return new EvalSuiteResult(suite, total, passed, passRate, aggregateScore, cases);
    }
}
