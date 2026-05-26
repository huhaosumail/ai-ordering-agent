package com.ximalaya.ai.ordering.evaluation.model;

import java.util.List;
import java.util.Map;

public record EvalCaseResult(
        String id,
        boolean passed,
        String message,
        Map<String, Object> details
) {
    public static EvalCaseResult pass(String id, String message, Map<String, Object> details) {
        return new EvalCaseResult(id, true, message, details);
    }

    public static EvalCaseResult fail(String id, String message, Map<String, Object> details) {
        return new EvalCaseResult(id, false, message, details);
    }

    public static Map<String, Object> detail(String key, Object value) {
        return Map.of(key, value);
    }

    public static Map<String, Object> detailsOf(Object... kv) {
        if (kv.length % 2 != 0) {
            throw new IllegalArgumentException("kv pairs required");
        }
        java.util.LinkedHashMap<String, Object> map = new java.util.LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            map.put(String.valueOf(kv[i]), kv[i + 1]);
        }
        return map;
    }

    @SuppressWarnings("unchecked")
    public List<String> getStringList(String key) {
        Object v = details != null ? details.get(key) : null;
        return v instanceof List<?> list ? (List<String>) list : List.of();
    }
}
