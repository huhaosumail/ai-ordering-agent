package com.ximalaya.ai.ordering.evaluation;

import com.ximalaya.ai.ordering.evaluation.model.EvalReport;
import com.ximalaya.ai.ordering.service.DishVectorIndexService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource(properties = {
        "ai.embedding.fallback-local=true",
        "rag.min-score=0.05",
        "eval.enabled=true",
        "eval.reindex-before-rag=false"
})
class EvaluationIntegrationTest {

    @Autowired
    EvaluationService evaluationService;

    @Autowired
    DishVectorIndexService dishVectorIndexService;

    @BeforeEach
    void indexSampleDishes() {
        dishVectorIndexService.reindexAll().block(Duration.ofSeconds(60));
    }

    @Test
    void agentIntentSuite_shouldPass() {
        EvalReport report = evaluationService.run("agent-intent", false)
                .block(Duration.ofSeconds(30));
        assertNotNull(report);
        assertEquals(6, report.agentIntent().total());
        assertTrue(report.agentIntent().passRate() >= 1.0,
                () -> "failures: " + report.agentIntent().cases().stream()
                        .filter(c -> !c.passed()).toList());
    }

    @Test
    void ragSuite_withLocalEmbed() {
        EvalReport report = evaluationService.run("rag", true)
                .block(Duration.ofSeconds(90));
        assertNotNull(report);
        assertTrue(report.rag().total() >= 4);
        assertTrue(report.rag().passRate() >= 0.75,
                () -> "rag pass rate too low: " + report.rag());
    }
}
