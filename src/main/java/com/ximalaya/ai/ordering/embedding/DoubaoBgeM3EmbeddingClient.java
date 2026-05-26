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
 * 火山 VikingDB Embedding v2 — 豆包平台 bge-m3 模型
 * 文档：https://www.volcengine.com/docs/84313/1254554
 */
public class DoubaoBgeM3EmbeddingClient implements EmbeddingClient {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private final EmbeddingProperties properties;
    private final OkHttpClient httpClient;

    public DoubaoBgeM3EmbeddingClient(EmbeddingProperties properties, OkHttpClient httpClient) {
        this.properties = properties;
        this.httpClient = httpClient;
    }

    @Override
    public float[] embed(String text) throws Exception {
        if (properties.getApiKey() == null || properties.getApiKey().isBlank()) {
            throw new IllegalStateException("未配置 VikingDB Embedding Token（ai.embedding.api-key 或 VIKINGDB_EMBEDDING_TOKEN）");
        }

        Map<String, Object> model = new HashMap<>();
        model.put("model_name", properties.getModelName());
        Map<String, Object> params = new HashMap<>();
        params.put("return_dense", properties.isReturnDense());
        params.put("return_sparse", properties.isReturnSparse());
        params.put("return_token_usage", false);
        if (properties.getEmbeddingDimension() > 0) {
            params.put("embedding_dimension", properties.getEmbeddingDimension());
        }
        model.put("params", params);

        Map<String, Object> item = new HashMap<>();
        item.put("data_type", "text");
        item.put("text", text);

        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("data", List.of(item));

        String url = trimTrailingSlash(properties.getHost()) + "/api/data/embedding/version/2";
        Request request = new Request.Builder()
                .url(url)
                .header("Content-Type", "application/json")
                .header("Authorization", buildAuthorization(properties.getApiKey()))
                .post(RequestBody.create(MAPPER.writeValueAsString(body), JSON))
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            String resp = response.body() != null ? response.body().string() : "";
            if (!response.isSuccessful()) {
                throw new IllegalStateException("HTTP " + response.code() + ": " + resp);
            }
            return VectorMath.l2Normalize(parseBgeM3Response(resp));
        }
    }

    @SuppressWarnings("unchecked")
    static float[] parseBgeM3Response(String json) throws Exception {
        Map<String, Object> root = MAPPER.readValue(json, new TypeReference<>() {});
        Number code = (Number) root.get("code");
        if (code != null && code.intValue() != 0) {
            throw new IllegalStateException("VikingDB embedding error: " + root.get("message"));
        }
        Map<String, Object> data = (Map<String, Object>) root.get("data");
        if (data == null) {
            throw new IllegalStateException("missing data in embedding response");
        }
        List<List<Number>> dense = (List<List<Number>>) data.get("sentence_dense_embedding");
        if (dense == null || dense.isEmpty() || dense.get(0) == null) {
            throw new IllegalStateException("empty sentence_dense_embedding");
        }
        List<Number> row = dense.get(0);
        float[] vector = new float[row.size()];
        for (int i = 0; i < row.size(); i++) {
            vector[i] = row.get(i).floatValue();
        }
        return vector;
    }

    /**
     * VikingDB 支持 token=xxx；若用户已带 Bearer 前缀则原样使用
     */
    static String buildAuthorization(String apiKey) {
        String key = apiKey.trim();
        if (key.startsWith("Bearer ") || key.startsWith("HMAC-SHA256") || key.startsWith("token=")) {
            return key;
        }
        return "token=" + key;
    }

    private static String trimTrailingSlash(String url) {
        if (url == null || url.isBlank()) {
            return "https://api-vikingdb.vikingdb.cn-beijing.volces.com";
        }
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
