package com.ximalaya.ai.ordering.evaluation;

import com.ximalaya.ai.ordering.evaluation.model.EvalCaseResult;
import com.ximalaya.ai.ordering.evaluation.model.EvalSuiteResult;
import com.ximalaya.ai.ordering.evaluation.model.RagEvalCase;
import com.ximalaya.ai.ordering.service.RagService;
import com.ximalaya.ai.ordering.vector.ScoredDocument;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

@Component
public class RagEvaluationRunner {

    private final RagService ragService;

    public RagEvaluationRunner(RagService ragService) {
        this.ragService = ragService;
    }

    public Mono<EvalSuiteResult> run(List<RagEvalCase> cases) {
        return Flux.fromIterable(cases)
                .concatMap(this::evaluateOne)
                .collectList()
                .map(results -> {
                    double mrr = results.stream()
                            .mapToDouble(r -> ((Number) r.details().getOrDefault("mrr", 0.0)).doubleValue())
                            .average()
                            .orElse(0.0);
                    return EvalSuiteResult.of("rag", results, mrr);
                });
    }

    private Mono<EvalCaseResult> evaluateOne(RagEvalCase evalCase) {
        return ragService.retrieve(evalCase.query())
                .map(docs -> scoreCase(evalCase, docs));
    }

    private EvalCaseResult scoreCase(RagEvalCase evalCase, List<ScoredDocument> docs) {
        List<String> retrieved = docs.stream().map(ScoredDocument::dishName).toList();
        int k = evalCase.effectiveTopK();
        boolean hit = EvalMetrics.hitAtK(retrieved, evalCase.expectedDishes(), k);
        double recall = EvalMetrics.recallAtK(retrieved, evalCase.expectedDishes(), k);
        double mrr = EvalMetrics.reciprocalRank(retrieved, evalCase.expectedDishes());
        boolean passed = hit && recall >= evalCase.effectiveMinRecall();

        List<String> scores = new ArrayList<>();
        for (ScoredDocument doc : docs) {
            scores.add(doc.dishName() + ":" + String.format("%.2f", doc.score()));
        }

        String message = passed
                ? "Hit@K 且 Recall 达标"
                : "未达标: hit=" + hit + ", recall=" + String.format("%.2f", recall);

        return passed
                ? EvalCaseResult.pass(evalCase.id(), message, EvalCaseResult.detailsOf(
                "query", evalCase.query(),
                "retrieved", retrieved,
                "expected", evalCase.expectedDishes(),
                "hitAtK", hit,
                "recallAtK", recall,
                "mrr", mrr,
                "scores", scores))
                : EvalCaseResult.fail(evalCase.id(), message, EvalCaseResult.detailsOf(
                "query", evalCase.query(),
                "retrieved", retrieved,
                "expected", evalCase.expectedDishes(),
                "hitAtK", hit,
                "recallAtK", recall,
                "mrr", mrr,
                "scores", scores));
    }
}
