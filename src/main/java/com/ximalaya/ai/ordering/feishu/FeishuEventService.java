package com.ximalaya.ai.ordering.feishu;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ximalaya.ai.ordering.agent.AgentService;
import com.ximalaya.ai.ordering.config.FeishuProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@ConditionalOnProperty(name = "feishu.enabled", havingValue = "true")
public class FeishuEventService {

    private static final Logger log = LoggerFactory.getLogger(FeishuEventService.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final FeishuProperties properties;
    private final FeishuClient feishuClient;
    private final AgentService agentService;
    private final ConcurrentHashMap<String, Boolean> processedEvents = new ConcurrentHashMap<>();

    public FeishuEventService(FeishuProperties properties,
                              FeishuClient feishuClient,
                              AgentService agentService) {
        this.properties = properties;
        this.feishuClient = feishuClient;
        this.agentService = agentService;
    }

    /**
     * 解析原始请求体（支持明文与 encrypt 字段加密）
     */
    public Mono<JsonNode> parsePayload(String rawBody) {
        return Mono.fromCallable(() -> {
            JsonNode root = MAPPER.readTree(rawBody);
            if (root.has("encrypt") && !properties.getEncryptKey().isBlank()) {
                String plain = FeishuCrypto.decrypt(properties.getEncryptKey(), root.get("encrypt").asText());
                return MAPPER.readTree(plain);
            }
            if (root.has("encrypt")) {
                throw new IllegalStateException("收到加密回调但未配置 feishu.encrypt-key");
            }
            return root;
        });
    }

    public Mono<Map<String, Object>> handlePayload(JsonNode payload) {
        if (payload.has("challenge")) {
            return Mono.just(Map.of("challenge", payload.get("challenge").asText()));
        }

        if (!verifyToken(payload)) {
            return Mono.error(new SecurityException("飞书 Verification Token 校验失败"));
        }

        if (payload.has("schema") && "2.0".equals(payload.get("schema").asText())) {
            return handleSchemaV2(payload);
        }

        return handleSchemaV1(payload);
    }

    private Mono<Map<String, Object>> handleSchemaV2(JsonNode payload) {
        JsonNode header = payload.path("header");
        String eventId = header.path("event_id").asText("");
        if (!eventId.isBlank() && processedEvents.putIfAbsent(eventId, Boolean.TRUE) != null) {
            log.debug("忽略重复飞书事件: {}", eventId);
            return Mono.just(Map.of());
        }

        String eventType = header.path("event_type").asText();
        if (!"im.message.receive_v1".equals(eventType)) {
            log.debug("忽略飞书事件类型: {}", eventType);
            return Mono.just(Map.of());
        }

        JsonNode event = payload.path("event");
        processMessageAsync(event);
        return Mono.just(Map.of());
    }

    private Mono<Map<String, Object>> handleSchemaV1(JsonNode payload) {
        String type = payload.path("type").asText();
        if ("url_verification".equals(type)) {
            return Mono.just(Map.of("challenge", payload.path("challenge").asText()));
        }

        JsonNode event = payload.path("event");
        if ("im.message.receive_v1".equals(event.path("type").asText())) {
            processMessageAsync(event);
        }
        return Mono.just(Map.of());
    }

    private void processMessageAsync(JsonNode event) {
        MessageContext ctx = extractMessage(event);
        if (ctx == null) {
            return;
        }

        Mono<String> replyMono;
        if (ctx.text().startsWith("/")) {
            replyMono = handleSlashCommand(ctx.text().trim(), ctx.sessionId())
                    .switchIfEmpty(agentService.chat(ctx.sessionId(), ctx.text(), null));
        } else {
            replyMono = agentService.chat(ctx.sessionId(), ctx.text(), null);
        }

        replyMono
                .flatMap(reply -> feishuClient.replyText(ctx.messageId(), reply))
                .doOnError(e -> log.error("处理飞书消息失败: sessionId={}", ctx.sessionId(), e))
                .onErrorResume(e -> feishuClient.replyText(ctx.messageId(),
                        "抱歉，处理您的消息时出错了，请稍后再试。"))
                .subscribe();
    }

    private MessageContext extractMessage(JsonNode event) {
        JsonNode message = event.path("message");
        String messageType = message.path("message_type").asText();
        if (!"text".equals(messageType)) {
            log.info("忽略非文本飞书消息: type={}", messageType);
            return null;
        }

        String senderType = event.path("sender").path("sender_type").asText();
        if ("app".equals(senderType)) {
            return null;
        }

        String messageId = message.path("message_id").asText();
        String chatId = message.path("chat_id").asText();
        if (messageId.isBlank() || chatId.isBlank()) {
            return null;
        }

        String text = parseTextContent(message.path("content").asText("{}"));
        if (text.isBlank()) {
            return null;
        }

        String sessionId = "feishu:" + chatId;
        log.info("飞书消息: sessionId={}, text={}", sessionId, text);
        return new MessageContext(messageId, sessionId, text);
    }

    private Mono<String> handleSlashCommand(String command, String sessionId) {
        if ("/clear".equalsIgnoreCase(command) || "/重置".equals(command)) {
            return agentService.clearSession(sessionId)
                    .thenReturn("对话已清除，可以重新开始点餐。");
        }
        if ("/help".equalsIgnoreCase(command) || "/帮助".equals(command)) {
            return Mono.just("""
                    智能点餐助手使用说明：
                    • 直接发送自然语言，例如：有什么辣的菜推荐？
                    • 下单示例：麻婆豆腐 三份、我要两份宫保鸡丁
                    • /clear 或 /重置 — 清除本会话记忆
                    • /help 或 /帮助 — 显示本说明
                    """);
        }
        return Mono.empty();
    }

    private String parseTextContent(String contentJson) {
        try {
            JsonNode content = MAPPER.readTree(contentJson);
            return content.path("text").asText("").trim();
        } catch (Exception e) {
            log.warn("解析飞书消息 content 失败: {}", contentJson);
            return "";
        }
    }

    private boolean verifyToken(JsonNode payload) {
        String configured = properties.getVerificationToken();
        if (configured == null || configured.isBlank()) {
            return true;
        }
        String token = payload.path("token").asText("");
        if (token.isBlank() && payload.has("header")) {
            token = payload.path("header").path("token").asText("");
        }
        return configured.equals(token);
    }

    public Mono<String> encryptResponseIfNeeded(Map<String, Object> body) {
        return Mono.fromCallable(() -> {
            String plain = writeJson(body);
            if (properties.getEncryptKey() == null || properties.getEncryptKey().isBlank()) {
                return plain;
            }
            String encrypt = FeishuCrypto.encrypt(properties.getEncryptKey(), plain);
            return MAPPER.writeValueAsString(Map.of("encrypt", encrypt));
        });
    }

    private String writeJson(Map<String, Object> body) throws Exception {
        return MAPPER.writeValueAsString(body);
    }

    private record MessageContext(String messageId, String sessionId, String text) {
    }
}
