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
 * 火山方舟向量化：文本 Embedding 用 /embeddings；多模态（doubao-embedding-vision）用 /embeddings/multimodal。
 */
public class DoubaoArkEmbeddingClient extends OpenAiCompatibleEmbeddingClient {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private final EmbeddingProperties properties;
    private final OkHttpClient httpClient;

    public DoubaoArkEmbeddingClient(EmbeddingProperties properties, OkHttpClient httpClient) {
        super(properties, httpClient);
        this.properties = properties;
        this.httpClient = httpClient;
    }

    @Override
    public float[] embed(String text) throws Exception {
        if (properties.isMultimodal()) {
            return embedMultimodal(text);
        }
        return super.embed(text);
    }

    private float[] embedMultimodal(String text) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("model", properties.getModel());
        body.put("encoding_format", "float");
        body.put("input", List.of(Map.of(
                "type", "text",
                "text", text)));

        String url = trimTrailingSlash(properties.getBaseUrl()) + "/embeddings/multimodal";
        Request request = new Request.Builder()
                .url(url)
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
            return VectorMath.l2Normalize(parseMultimodalEmbedding(resp));
        }
    }

    @SuppressWarnings("unchecked")
    static float[] parseMultimodalEmbedding(String json) throws Exception {
        Map<String, Object> root = MAPPER.readValue(json, new TypeReference<>() {});
        Object data = root.get("data");
        if (data instanceof Map<?, ?> dataMap) {
            Object embedding = dataMap.get("embedding");
            if (embedding instanceof List<?> list) {
                return toFloatArray(list);
            }
        }
        // 兼容 OpenAI 风格 data: [{embedding: [...]}]
        return OpenAiCompatibleEmbeddingClient.parseOpenAiEmbedding(json);
    }

    private static float[] toFloatArray(List<?> values) {
        float[] vector = new float[values.size()];
        for (int i = 0; i < values.size(); i++) {
            vector[i] = ((Number) values.get(i)).floatValue();
        }
        return vector;
    }

    private static String trimTrailingSlash(String url) {
        if (url == null) {
            return "";
        }
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
