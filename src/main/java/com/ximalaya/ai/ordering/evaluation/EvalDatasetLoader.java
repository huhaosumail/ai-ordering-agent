package com.ximalaya.ai.ordering.evaluation;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ximalaya.ai.ordering.evaluation.model.AgentIntentEvalCase;
import com.ximalaya.ai.ordering.evaluation.model.RagEvalCase;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.List;

@Component
public class EvalDatasetLoader {

    private static final String RAG_PATH = "eval/rag-golden.json";
    private static final String AGENT_PATH = "eval/agent-intent-golden.json";

    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<RagEvalCase> loadRagCases() {
        return load(RAG_PATH, new TypeReference<>() {});
    }

    public List<AgentIntentEvalCase> loadAgentIntentCases() {
        return load(AGENT_PATH, new TypeReference<>() {});
    }

    private <T> T load(String path, TypeReference<T> type) {
        try (InputStream in = new ClassPathResource(path).getInputStream()) {
            return objectMapper.readValue(in, type);
        } catch (Exception e) {
            throw new IllegalStateException("无法加载评估数据集: " + path, e);
        }
    }
}
