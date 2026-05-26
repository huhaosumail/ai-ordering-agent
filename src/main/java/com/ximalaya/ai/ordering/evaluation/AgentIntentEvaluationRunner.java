package com.ximalaya.ai.ordering.evaluation;

import com.ximalaya.ai.ordering.agent.AgentIntentMatcher;
import com.ximalaya.ai.ordering.evaluation.model.AgentIntentEvalCase;
import com.ximalaya.ai.ordering.evaluation.model.EvalCaseResult;
import com.ximalaya.ai.ordering.evaluation.model.EvalSuiteResult;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
public class AgentIntentEvaluationRunner {

    private final AgentIntentMatcher agentIntentMatcher;

    public AgentIntentEvaluationRunner(AgentIntentMatcher agentIntentMatcher) {
        this.agentIntentMatcher = agentIntentMatcher;
    }

    public Mono<EvalSuiteResult> run(List<AgentIntentEvalCase> cases) {
        List<EvalCaseResult> results = cases.stream()
                .map(this::evaluateOne)
                .toList();
        return Mono.just(EvalSuiteResult.of("agent-intent", results, aggregateAccuracy(results)));
    }

    private EvalCaseResult evaluateOne(AgentIntentEvalCase evalCase) {
        String actual = agentIntentMatcher.match(evalCase.message(), 1L)
                .map(AgentIntentMatcher.MatchedIntent::toolName)
                .orElse("(none)");
        boolean passed = evalCase.expectedTool().equals(actual);
        String message = passed
                ? "工具匹配"
                : "期望 " + evalCase.expectedTool() + "，实际 " + actual;
        return passed
                ? EvalCaseResult.pass(evalCase.id(), message, EvalCaseResult.detailsOf(
                "message", evalCase.message(),
                "expectedTool", evalCase.expectedTool(),
                "actualTool", actual))
                : EvalCaseResult.fail(evalCase.id(), message, EvalCaseResult.detailsOf(
                "message", evalCase.message(),
                "expectedTool", evalCase.expectedTool(),
                "actualTool", actual));
    }

    private double aggregateAccuracy(List<EvalCaseResult> results) {
        if (results.isEmpty()) {
            return 1.0;
        }
        long passed = results.stream().filter(EvalCaseResult::passed).count();
        return (double) passed / results.size();
    }
}
