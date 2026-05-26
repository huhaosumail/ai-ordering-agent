package com.ximalaya.ai.ordering.controller;

import com.ximalaya.ai.ordering.config.EvalProperties;
import com.ximalaya.ai.ordering.dto.response.ApiResponse;
import com.ximalaya.ai.ordering.evaluation.EvaluationService;
import com.ximalaya.ai.ordering.evaluation.model.EvalReport;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/eval")
@ConditionalOnProperty(name = "eval.enabled", havingValue = "true", matchIfMissing = true)
public class EvalController {

    private final EvaluationService evaluationService;
    private final EvalProperties evalProperties;

    public EvalController(EvaluationService evaluationService, EvalProperties evalProperties) {
        this.evaluationService = evaluationService;
        this.evalProperties = evalProperties;
    }

    /**
     * 运行评估套件
     *
     * @param suite all | rag | agent-intent
     */
    @PostMapping("/run")
    public Mono<ResponseEntity<ApiResponse<EvalReport>>> run(
            @RequestParam(defaultValue = "all") String suite,
            @RequestParam(required = false) Boolean reindex) {
        boolean doReindex = reindex != null ? reindex : evalProperties.isReindexBeforeRag();
        return evaluationService.run(suite, doReindex)
                .map(report -> ResponseEntity.ok(ApiResponse.success(report)));
    }

    @GetMapping("/info")
    public Mono<ResponseEntity<ApiResponse<Object>>> info() {
        return Mono.just(ResponseEntity.ok(ApiResponse.success(java.util.Map.of(
                "suites", java.util.List.of("rag", "agent-intent", "all"),
                "ragMetrics", java.util.List.of("hitAtK", "recallAtK", "mrr"),
                "agentNote", "基于 AgentIntentMatcher（模拟模式路由规则）",
                "datasets", java.util.List.of("classpath:eval/rag-golden.json", "classpath:eval/agent-intent-golden.json")
        ))));
    }
}
