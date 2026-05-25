package com.ximalaya.ai.ordering.feishu;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ximalaya.ai.ordering.config.FeishuProperties;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

@Component
@ConditionalOnProperty(name = "feishu.enabled", havingValue = "true")
public class FeishuClient {

    private static final Logger log = LoggerFactory.getLogger(FeishuClient.class);
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final FeishuProperties properties;
    private final OkHttpClient httpClient;
    private final AtomicReference<TokenCache> tokenCache = new AtomicReference<>();

    public FeishuClient(FeishuProperties properties) {
        this.properties = properties;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(Duration.ofSeconds(15))
                .readTimeout(Duration.ofSeconds(30))
                .writeTimeout(Duration.ofSeconds(15))
                .build();
    }

    public Mono<Void> replyText(String messageId, String text) {
        return tenantAccessToken()
                .flatMap(token -> Mono.fromRunnable(() -> {
                    try {
                        doReply(token, messageId, text);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }))
                .then()
                .onErrorResume(e -> {
                    log.error("飞书回复消息失败: messageId={}", messageId, e);
                    return Mono.empty();
                });
    }

    private void doReply(String token, String messageId, String text) throws Exception {
        String content = MAPPER.writeValueAsString(Map.of("text", text));
        String body = MAPPER.writeValueAsString(Map.of(
                "msg_type", "text",
                "content", content
        ));
        String url = properties.getBaseUrl() + "/open-apis/im/v1/messages/" + messageId + "/reply";
        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer " + token)
                .post(RequestBody.create(body, JSON))
                .build();
        try (Response response = httpClient.newCall(request).execute()) {
            String respBody = response.body() != null ? response.body().string() : "";
            if (!response.isSuccessful()) {
                throw new IllegalStateException("HTTP " + response.code() + ": " + respBody);
            }
            JsonNode json = MAPPER.readTree(respBody);
            if (json.path("code").asInt(-1) != 0) {
                throw new IllegalStateException("Feishu API error: " + respBody);
            }
            log.debug("飞书回复成功: messageId={}", messageId);
        }
    }

    private Mono<String> tenantAccessToken() {
        return Mono.fromCallable(this::resolveTenantAccessToken);
    }

    private String resolveTenantAccessToken() throws Exception {
        TokenCache cached = tokenCache.get();
        if (cached != null && cached.isValid()) {
            return cached.token();
        }
        synchronized (this) {
            cached = tokenCache.get();
            if (cached != null && cached.isValid()) {
                return cached.token();
            }
            String token = fetchTenantAccessToken();
            tokenCache.set(new TokenCache(token, Instant.now().plusSeconds(7000)));
            return token;
        }
    }

    private String fetchTenantAccessToken() throws Exception {
        String body = MAPPER.writeValueAsString(Map.of(
                "app_id", properties.getAppId(),
                "app_secret", properties.getAppSecret()
        ));
        String url = properties.getBaseUrl() + "/open-apis/auth/v3/tenant_access_token/internal";
        Request request = new Request.Builder()
                .url(url)
                .post(RequestBody.create(body, JSON))
                .build();
        try (Response response = httpClient.newCall(request).execute()) {
            String respBody = response.body() != null ? response.body().string() : "";
            JsonNode json = MAPPER.readTree(respBody);
            if (json.path("code").asInt(-1) != 0) {
                throw new IllegalStateException("获取 tenant_access_token 失败: " + respBody);
            }
            return json.path("tenant_access_token").asText();
        }
    }

    private record TokenCache(String token, Instant expiresAt) {
        boolean isValid() {
            return Instant.now().isBefore(expiresAt);
        }
    }
}
