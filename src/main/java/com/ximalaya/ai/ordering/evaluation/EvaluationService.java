package com.ximalaya.ai.ordering.evaluation;

import com.ximalaya.ai.ordering.evaluation.model.EvalReport;
import com.ximalaya.ai.ordering.evaluation.model.EvalSuiteResult;
import com.ximalaya.ai.ordering.service.DishVectorIndexService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class EvaluationService {

    private static final Logger log = LoggerFactory.getLogger(EvaluationService.class);

    private final EvalDatasetLoader datasetLoader;
    private final RagEvaluationRunner ragEvaluationRunner;
    private final AgentIntentEvaluationRunner agentIntentEvaluationRunner;
    private final DishVectorIndexService dishVectorIndexService;

    public EvaluationService(EvalDatasetLoader datasetLoader,
                             RagEvaluationRunner ragEvaluationRunner,
                             AgentIntentEvaluationRunner agentIntentEvaluationRunner,
                             DishVectorIndexService dishVectorIndexService) {
        this.datasetLoader = datasetLoader;
        this.ragEvaluationRunner = ragEvaluationRunner;
        this.agentIntentEvaluationRunner = agentIntentEvaluationRunner;
        this.dishVectorIndexService = dishVectorIndexService;
    }

    /**
     * @param suite rag | agent-intent | all
     */
    public Mono<EvalReport> run(String suite, boolean reindexBeforeRag) {
        String normalized = suite == null ? "all" : suite.trim().toLowerCase();
        Mono<EvalSuiteResult> ragMono = Mono.empty();
        Mono<EvalSuiteResult> agentMono = Mono.empty();

        if ("all".equals(normalized) || "rag".equals(normalized)) {
            Mono<Void> prepare = reindexBeforeRag
                    ? dishVectorIndexService.reindexAll()
                    : Mono.empty();
            ragMono = prepare.then(ragEvaluationRunner.run(datasetLoader.loadRagCases()));
        }
        if ("all".equals(normalized) || "agent-intent".equals(normalized) || "agent".equals(normalized)) {
            agentMono = agentIntentEvaluationRunner.run(datasetLoader.loadAgentIntentCases());
        }

        return Mono.zip(
                        ragMono.defaultIfEmpty(emptySuite("rag")),
                        agentMono.defaultIfEmpty(emptySuite("agent-intent")))
                .map(tuple -> {
                    EvalReport report = EvalReport.of(tuple.getT1(), tuple.getT2());
                    log.info("评估完成: rag {}/{} agent {}/{} allPassed={}",
                            report.rag().passed(), report.rag().total(),
                            report.agentIntent().passed(), report.agentIntent().total(),
                            report.allPassed());
                    return report;
                });
    }

    private static EvalSuiteResult emptySuite(String name) {
        return EvalSuiteResult.of(name, java.util.List.of(), 0.0);
    }
}
