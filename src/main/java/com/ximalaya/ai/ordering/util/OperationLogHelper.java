package com.ximalaya.ai.ordering.util;

import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpRequest;

public final class OperationLogHelper {

    public static final String TRACE_ID_HEADER = "X-Trace-Id";
    public static final String TRACE_ID_ATTR = "operationLog.traceId";
    public static final String REQUEST_BODY_ATTR = "operationLog.requestBody";
    public static final String ERROR_ATTR = "operationLog.error";
    public static final int MAX_PARAM_LENGTH = 4096;

    private OperationLogHelper() {}

    public static String resolveModule(String path) {
        if (path == null) {
            return "SYSTEM";
        }
        if (path.startsWith("/api/ai")) {
            return "AI";
        }
        if (path.startsWith("/api/orders")) {
            return "ORDER";
        }
        if (path.startsWith("/api/dishes")) {
            return "DISH";
        }
        if (path.startsWith("/api/categories")) {
            return "CATEGORY";
        }
        if (path.startsWith("/api/logs")) {
            return "LOG";
        }
        return "SYSTEM";
    }

    public static String resolveAction(String method, String path) {
        if (path == null) {
            return method;
        }
        if (path.contains("/order/parse")) {
            return "AI_PARSE_ORDER";
        }
        if (path.equals("/api/ai/order") || path.endsWith("/api/ai/order")) {
            return "AI_CREATE_ORDER";
        }
        if (path.contains("/recommend")) {
            return "AI_RECOMMEND";
        }
        if (path.contains("/status")) {
            return "UPDATE_ORDER_STATUS";
        }
        if (path.contains("/top-sales")) {
            return "QUERY_TOP_SALES";
        }
        if (path.contains("/top-rated")) {
            return "QUERY_TOP_RATED";
        }
        if ("POST".equals(method)) {
            return "CREATE";
        }
        if ("PUT".equals(method) || "PATCH".equals(method)) {
            return "UPDATE";
        }
        if ("DELETE".equals(method)) {
            return "DELETE";
        }
        if ("GET".equals(method)) {
            return "QUERY";
        }
        return method + "_" + path.replace("/", "_").replace("-", "_");
    }

    public static Long extractUserId(ServerHttpRequest request, String requestBody) {
        String query = request.getURI().getQuery();
        if (query != null) {
            Long fromQuery = parseUserIdFromText(query);
            if (fromQuery != null) {
                return fromQuery;
            }
        }
        String path = request.getURI().getPath();
        if (path != null && path.contains("/user/")) {
            String[] parts = path.split("/");
            for (int i = 0; i < parts.length - 1; i++) {
                if ("user".equals(parts[i])) {
                    try {
                        return Long.parseLong(parts[i + 1]);
                    } catch (NumberFormatException ignored) {
                        return null;
                    }
                }
            }
        }
        if (requestBody != null) {
            return parseUserIdFromText(requestBody);
        }
        return null;
    }

    private static Long parseUserIdFromText(String text) {
        int idx = text.indexOf("userId");
        if (idx < 0) {
            return null;
        }
        int start = text.indexOf(':', idx);
        if (start < 0) {
            start = text.indexOf('=', idx);
        }
        if (start < 0) {
            return null;
        }
        StringBuilder digits = new StringBuilder();
        for (int i = start + 1; i < text.length(); i++) {
            char c = text.charAt(i);
            if (Character.isDigit(c)) {
                digits.append(c);
            } else if (digits.length() > 0) {
                break;
            }
        }
        if (digits.isEmpty()) {
            return null;
        }
        try {
            return Long.parseLong(digits.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static String truncate(String value) {
        if (value == null) {
            return null;
        }
        if (value.length() <= MAX_PARAM_LENGTH) {
            return value;
        }
        return value.substring(0, MAX_PARAM_LENGTH) + "...(truncated)";
    }

    public static String buildRequestParams(ServerHttpRequest request, String body) {
        String query = request.getURI().getQuery();
        if (body != null && !body.isBlank()) {
            if (query != null && !query.isBlank()) {
                return truncate("query=" + query + "&body=" + body);
            }
            return truncate(body);
        }
        return query != null ? truncate(query) : null;
    }

    public static boolean hasRequestBody(HttpMethod method) {
        return HttpMethod.POST.equals(method)
                || HttpMethod.PUT.equals(method)
                || HttpMethod.PATCH.equals(method);
    }
}
