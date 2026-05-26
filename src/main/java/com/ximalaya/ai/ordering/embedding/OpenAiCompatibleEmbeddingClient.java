package com.ximalaya.ai.ordering.embedding;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ximalaya.ai.ordering.config.EmbeddingProperties;
import com.ximalaya.ai.ordering.vector.VectorMath;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * OpenAI 兼容 /embeddings（也用于通用第三方）
 */
public class OpenAiCompatibleEmbeddingClient implements EmbeddingClient {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private final EmbeddingProperties properties;
    private final OkHttpClient httpClient;

    public OpenAiCompatibleEmbeddingClient(EmbeddingProperties properties, OkHttpClient httpClient) {
        this.properties = properties;
        this.httpClient = httpClient;
    }

    @Override
    public float[] embed(String text) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("model", properties.getModel());
        body.put("input", text);
        body.put("encoding_format", "float");

        String baseUrl = trimTrailingSlash(properties.getBaseUrl());
        Request request = new Request.Builder()
                .url(baseUrl + "/embeddings")
                .header("Authorization", "Bearer " + properties.getApiKey())
                .header("Content-Type", "application/json")
                .post(RequestBody.create(MAPPER.writeValueAsString(body), JSON))
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String err = response.body() != null ? response.body().string() : "";
                throw new IllegalStateException("HTTP " + response.code() + ": " + err);
            }
            String resp = response.body() != null ? response.body().string() : "";
            return VectorMath.l2Normalize(parseOpenAiEmbedding(resp));
        }
    }

    @SuppressWarnings("unchecked")
    static float[] parseOpenAiEmbedding(String json) throws Exception {
        Map<String, Object> root = MAPPER.readValue(json, new TypeReference<>() {});
        List<Map<String, Object>> data = (List<Map<String, Object>>) root.get("data");
        if (data == null || data.isEmpty()) {
            throw new IllegalStateException("empty embedding data");
        }
        Object raw = data.get(0).get("embedding");
        List<?> values = flattenEmbedding(raw);
        float[] vector = new float[values.size()];
        for (int i = 0; i < values.size(); i++) {
            vector[i] = ((Number) values.get(i)).floatValue();
        }
        return vector;
    }

    @SuppressWarnings("unchecked")
    private static List<?> flattenEmbedding(Object raw) {
        if (raw instanceof List<?> list) {
            if (!list.isEmpty() && list.get(0) instanceof List<?>) {
                return (List<?>) list.get(0);
            }
            return list;
        }
        throw new IllegalStateException("unsupported embedding format");
    }

    private static String trimTrailingSlash(String url) {
        if (url == null) {
            return "";
        }
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
