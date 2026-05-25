package com.ximalaya.ai.ordering.agent.impl;

import com.ximalaya.ai.ordering.agent.AgentService;
import com.ximalaya.ai.ordering.agent.tool.CategoryQueryTool;
import com.ximalaya.ai.ordering.agent.tool.DishQueryTool;
import com.ximalaya.ai.ordering.agent.tool.OrderQueryTool;
import com.ximalaya.ai.ordering.agent.tool.Tool;
import com.ximalaya.ai.ordering.service.impl.ChatMemoryServiceImpl;
import com.ximalaya.ai.ordering.service.OperationLogService;
import com.ximalaya.ai.ordering.util.OperationLogHelper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.*;

@Service
public class AgentServiceImpl implements AgentService {

    private static final Logger log = LoggerFactory.getLogger(AgentServiceImpl.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final ChatMemoryServiceImpl chatMemoryService;
    private final OperationLogService operationLogService;
    private final OkHttpClient httpClient;
    private final String apiKey;
    private final String baseUrl;
    private final String model;

    private final List<Tool> tools;
    private static final int MAX_HISTORY_MESSAGES = 10;

    private static final String SYSTEM_PROMPT = """
            你是一个智能点餐助手，你可以使用工具来查询菜品、订单和分类信息。
            
            可用工具：
            1. query_dishes - 查询菜品信息
               参数：keyword(可选) - 菜品名称关键词；category(可选) - 分类名称
            2. query_orders - 查询订单信息
               参数：orderNo(可选) - 订单号；userId(可选) - 用户ID；status(可选) - 订单状态
            3. query_categories - 查询所有分类
            
            工具调用格式：<function name="工具名" params="参数JSON">
            
            例如：<function name="query_dishes" params="{\"keyword\":\"鸡丁\"}">
            
            思考过程：你需要根据用户的问题判断是否需要调用工具。如果需要调用工具，请输出工具调用；如果不需要，可以直接回答用户。
            
            当收到工具执行结果后，请用自然、友好的语言总结给用户。
            """;

    public AgentServiceImpl(ChatMemoryServiceImpl chatMemoryService,
                           OperationLogService operationLogService,
                           DishQueryTool dishQueryTool,
                           OrderQueryTool orderQueryTool,
                           CategoryQueryTool categoryQueryTool,
                           @Value("${ai.deepseek.api-key}") String apiKey,
                           @Value("${ai.deepseek.base-url}") String baseUrl,
                           @Value("${ai.deepseek.model}") String model) {
        this.chatMemoryService = chatMemoryService;
        this.operationLogService = operationLogService;
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
        this.model = model;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(java.time.Duration.ofSeconds(30))
                .readTimeout(java.time.Duration.ofSeconds(60))
                .writeTimeout(java.time.Duration.ofSeconds(30))
                .build();
        this.tools = Arrays.asList(dishQueryTool, orderQueryTool, categoryQueryTool);
    }

    @Override
    public Mono<String> chat(String sessionId, String userInput, Long userId) {
        log.info("Agent聊天: sessionId={}, userInput={}", sessionId, userInput);

        return chatMemoryService.addUserMessage(sessionId, userInput, userId)
                .then(chatMemoryService.getMessagesForLlm(sessionId, MAX_HISTORY_MESSAGES))
                .flatMap(messages -> {
                    String prompt = buildPrompt(messages, userInput);
                    return Mono.fromCallable(() -> callDeepSeekWithTools(prompt))
                            .flatMap(response -> processResponse(sessionId, response));
                });
    }

    private String buildPrompt(List<Map<String, String>> history, String currentInput) {
        StringBuilder prompt = new StringBuilder();
        prompt.append(SYSTEM_PROMPT).append("\n\n");
        
        prompt.append("对话历史：\n");
        for (Map<String, String> message : history) {
            prompt.append(message.get("role")).append(": ").append(message.get("content")).append("\n");
        }
        
        prompt.append("\n用户最新提问：").append(currentInput).append("\n");
        prompt.append("\n请根据对话历史和用户当前提问，决定是否调用工具。如需调用工具，请输出工具调用格式；如不需要，直接回答用户。");
        
        return prompt.toString();
    }

    private String callDeepSeekWithTools(String prompt) {
        long start = System.currentTimeMillis();
        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            
            List<Map<String, String>> messages = new ArrayList<>();
            Map<String, String> userMessage = new HashMap<>();
            userMessage.put("role", "user");
            userMessage.put("content", prompt);
            messages.add(userMessage);
            requestBody.put("messages", messages);
            
            requestBody.put("temperature", 0.7);
            requestBody.put("max_tokens", 2048);

            String jsonBody = objectMapper.writeValueAsString(requestBody);

            RequestBody body = RequestBody.create(jsonBody, 
                    okhttp3.MediaType.parse("application/json"));

            Request request = new Request.Builder()
                    .url(baseUrl + "/chat/completions")
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .post(body)
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    log.error("DeepSeek API调用失败: {}", response.code());
                    recordAiCall(prompt, false, "HTTP " + response.code(), System.currentTimeMillis() - start, null);
                    return "{\"error\":\"系统繁忙\"}";
                }

                String responseBody = response.body() != null ? response.body().string() : "";
                String content = extractContent(responseBody);
                recordAiCall(prompt, true, null, System.currentTimeMillis() - start, null);
                return content;
            }
        } catch (Exception e) {
            log.error("DeepSeek API调用异常: {}", e.getMessage());
            recordAiCall(prompt, false, e.getMessage(), System.currentTimeMillis() - start, null);
            return "{\"error\":\"系统繁忙\"}";
        }
    }

    private Mono<String> processResponse(String sessionId, String response) {
        log.debug("LLM响应: {}", response);
        
        List<ToolCall> toolCalls = parseToolCalls(response);
        
        if (toolCalls.isEmpty()) {
            chatMemoryService.addAssistantMessage(sessionId, response).subscribe();
            return Mono.just(response);
        }
        
        return executeTools(toolCalls)
                .flatMap(toolResults -> {
                    StringBuilder toolResultSummary = new StringBuilder();
                    for (int i = 0; i < toolCalls.size(); i++) {
                        toolResultSummary.append("工具[").append(toolCalls.get(i).name).append("]执行结果：\n");
                        toolResultSummary.append(toolResults.get(i)).append("\n");
                    }
                    
                    chatMemoryService.addToolMessage(sessionId, toolCalls.get(0).name, toolResultSummary.toString()).subscribe();
                    
                    String summaryPrompt = SYSTEM_PROMPT + "\n\n" +
                            "工具执行结果：\n" + toolResultSummary + "\n\n" +
                            "请根据工具执行结果，用自然、友好的语言总结给用户。";
                    
                    return Mono.fromCallable(() -> callDeepSeekWithTools(summaryPrompt))
                            .doOnNext(summary -> chatMemoryService.addAssistantMessage(sessionId, summary).subscribe());
                });
    }

    private List<ToolCall> parseToolCalls(String response) {
        List<ToolCall> toolCalls = new ArrayList<>();
        String pattern = "<function name=\"(.*?)\" params=\"(.*?)\">";
        
        java.util.regex.Pattern r = java.util.regex.Pattern.compile(pattern);
        java.util.regex.Matcher m = r.matcher(response);
        
        while (m.find()) {
            String toolName = m.group(1);
            String paramsJson = m.group(2);
            try {
                Map<String, Object> params = objectMapper.readValue(paramsJson, new TypeReference<Map<String, Object>>() {});
                toolCalls.add(new ToolCall(toolName, params));
            } catch (Exception e) {
                log.warn("解析工具参数失败: {}", e.getMessage());
            }
        }
        return toolCalls;
    }

    private Mono<List<String>> executeTools(List<ToolCall> toolCalls) {
        List<Mono<String>> results = new ArrayList<>();
        
        for (ToolCall call : toolCalls) {
            Mono<String> result = tools.stream()
                    .filter(t -> t.getName().equals(call.name))
                    .findFirst()
                    .map(tool -> tool.execute(call.params))
                    .orElse(Mono.just("未知工具: " + call.name));
            results.add(result);
        }
        
        return Mono.zip(results, objects -> {
            List<String> stringResults = new ArrayList<>();
            for (Object obj : objects) {
                stringResults.add((String) obj);
            }
            return stringResults;
        });
    }

    private String extractContent(String responseJson) {
        try {
            Map<String, Object> response = objectMapper.readValue(responseJson, new TypeReference<Map<String, Object>>() {});
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
            if (choices != null && !choices.isEmpty()) {
                Map<String, Object> choice = choices.get(0);
                @SuppressWarnings("unchecked")
                Map<String, String> message = (Map<String, String>) choice.get("message");
                if (message != null) {
                    return message.get("content");
                }
            }
        } catch (Exception e) {
            log.warn("解析DeepSeek响应失败: {}", e.getMessage());
        }
        return responseJson;
    }

    private void recordAiCall(String prompt, boolean success, String errorMessage, long durationMs, Long userId) {
        operationLogService.recordInternal(
                "AI",
                "AGENT_CHAT",
                OperationLogHelper.truncate(prompt),
                success,
                errorMessage,
                durationMs,
                userId
        ).subscribe();
    }

    @Override
    public Mono<String> getSessionSummary(String sessionId) {
        return chatMemoryService.getSessionHistory(sessionId, MAX_HISTORY_MESSAGES)
                .map(history -> {
                    if (history.isEmpty()) {
                        return "暂无对话记录";
                    }
                    return "对话摘要：\n" + history;
                });
    }

    @Override
    public Mono<Void> clearSession(String sessionId) {
        return chatMemoryService.clearSession(sessionId);
    }

    private record ToolCall(String name, Map<String, Object> params) {}
}