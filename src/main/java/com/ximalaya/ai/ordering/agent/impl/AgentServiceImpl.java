package com.ximalaya.ai.ordering.agent.impl;

import com.ximalaya.ai.ordering.agent.AgentService;
import com.ximalaya.ai.ordering.agent.tool.CategoryQueryTool;
import com.ximalaya.ai.ordering.agent.tool.CreateOrderTool;
import com.ximalaya.ai.ordering.agent.tool.DishQueryTool;
import com.ximalaya.ai.ordering.agent.tool.DishSalesRankTool;
import com.ximalaya.ai.ordering.agent.tool.OrderQueryTool;
import com.ximalaya.ai.ordering.agent.tool.SemanticDishSearchTool;
import com.ximalaya.ai.ordering.agent.tool.Tool;
import com.ximalaya.ai.ordering.service.RagService;
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
    private final boolean simulationMode;
    private static final int MAX_HISTORY_MESSAGES = 10;

    private static final String SYSTEM_PROMPT = """
            你是一个智能点餐助手，你可以使用工具来查询菜品、订单和分类信息。
            
            可用工具：
            1. query_dishes - 按关键词/分类精确查询菜品
               参数：keyword(可选) - 菜品名称关键词；category(可选) - 分类名称
            2. semantic_search_dishes - 按语义/口味/场景向量检索（RAG）
               参数：query(必填) - 如「辣的」「下饭」「清淡」「适合约会」
            3. query_orders - 查询订单信息
               参数：orderNo(可选) - 订单号；userId(可选) - 用户ID；status(可选) - 订单状态
            4. query_categories - 查询所有分类
            5. query_dishes_sales_rank - 查询销量排行榜（畅销、卖得最好）
               参数：limit(可选) - 返回条数，默认 10，最大 20
            6. create_order - 创建订单（用户明确要点菜、下单时使用）
               参数：items(必填) - [{\"name\":\"菜品名\",\"quantity\":数量}]；userId(可选)；tableNo(可选)；remark(可选)
               示例：用户说「麻婆豆腐三份」「两份宫保鸡丁」时必须调用 create_order，不要只回复问候语。
            
            工具调用格式：<function name="工具名" params="参数JSON">
            
            例如：<function name="create_order" params='{"items":[{"name":"麻婆豆腐","quantity":3}]}'>
            
            例如：<function name="query_dishes" params="{\"keyword\":\"鸡丁\"}">
            
            思考过程：你需要根据用户的问题判断是否需要调用工具。如果需要调用工具，请输出工具调用；如果不需要，可以直接回答用户。
            
            当用户问销量、畅销、卖得最好、排行榜时，使用 query_dishes_sales_rank。
            当用户用口味/场景/模糊描述找菜时，优先使用 semantic_search_dishes；已知菜名时用 query_dishes。
            系统可能附带【RAG 检索到的相关菜品】上下文，请结合使用。
            
            当收到工具执行结果后，请用自然、友好的语言总结给用户。
            """;

    private final RagService ragService;

    public AgentServiceImpl(ChatMemoryServiceImpl chatMemoryService,
                           OperationLogService operationLogService,
                           DishQueryTool dishQueryTool,
                           DishSalesRankTool dishSalesRankTool,
                           SemanticDishSearchTool semanticDishSearchTool,
                           OrderQueryTool orderQueryTool,
                           CategoryQueryTool categoryQueryTool,
                           CreateOrderTool createOrderTool,
                           RagService ragService,
                           @Value("${ai.deepseek.api-key}") String apiKey,
                           @Value("${ai.deepseek.base-url}") String baseUrl,
                           @Value("${ai.deepseek.model}") String model,
                           @Value("${agent.ordering.simulation-mode:false}") boolean simulationMode) {
        this.chatMemoryService = chatMemoryService;
        this.operationLogService = operationLogService;
        this.ragService = ragService;
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
        this.model = model;
        this.simulationMode = simulationMode;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(java.time.Duration.ofSeconds(30))
                .readTimeout(java.time.Duration.ofSeconds(60))
                .writeTimeout(java.time.Duration.ofSeconds(30))
                .build();
        this.tools = Arrays.asList(
                dishQueryTool, dishSalesRankTool, semanticDishSearchTool,
                orderQueryTool, categoryQueryTool, createOrderTool);
    }

    @Override
    public Mono<String> chat(String sessionId, String userInput, Long userId) {
        log.info("Agent聊天: sessionId={}, userInput={}", sessionId, userInput);

        return chatMemoryService.addUserMessage(sessionId, userInput, userId)
                .then(chatMemoryService.getMessagesForLlm(sessionId, MAX_HISTORY_MESSAGES))
                .flatMap(messages -> buildPrompt(messages, userInput)
                        .flatMap(prompt -> Mono.fromCallable(() -> callDeepSeekWithTools(prompt, userId))
                                .flatMap(response -> processResponse(sessionId, response, userId, userInput))));
    }

    private Mono<String> buildPrompt(List<Map<String, String>> history, String currentInput) {
        return ragService.buildAgentContext(currentInput)
                .map(ctx -> assemblePrompt(history, currentInput, ctx));
    }

    private String assemblePrompt(List<Map<String, String>> history, String currentInput, String ragContext) {
        StringBuilder prompt = new StringBuilder();
        prompt.append(SYSTEM_PROMPT).append("\n\n");

        if (ragContext != null && !ragContext.isBlank()) {
            prompt.append(ragContext).append("\n");
        }

        prompt.append("对话历史：\n");
        for (Map<String, String> message : history) {
            prompt.append(message.get("role")).append(": ").append(message.get("content")).append("\n");
        }
        
        prompt.append("\n用户最新提问：").append(currentInput).append("\n");
        prompt.append("\n请根据对话历史和用户当前提问，决定是否调用工具。如需调用工具，请输出工具调用格式；如不需要，直接回答用户。");

        return prompt.toString();
    }

    private String callDeepSeekWithTools(String prompt, Long userId) {
        if (simulationMode) {
            log.debug("Agent 使用本地模拟模式");
            return simulateLlmResponse(prompt, userId);
        }
        
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
                    return simulateLlmResponse(prompt, userId);
                }

                String responseBody = response.body() != null ? response.body().string() : "";
                String content = extractContent(responseBody);
                recordAiCall(prompt, true, null, System.currentTimeMillis() - start, null);
                return content;
            }
        } catch (Exception e) {
            log.error("DeepSeek API调用异常: {}", e.getMessage());
            recordAiCall(prompt, false, e.getMessage(), System.currentTimeMillis() - start, null);
            return simulateLlmResponse(prompt, userId);
        }
    }

    private String simulateLlmResponse(String prompt, Long userId) {
        log.info("使用模拟模式响应");

        if (prompt.contains("【工具执行结果】")) {
            return summarizeToolResults(prompt);
        }

        String userMsg = extractLatestUserMessage(prompt);

        // 下单意图优先（避免对话历史里的「麻辣」误触发查菜）
        Optional<Map<String, Object>> orderParams = tryParseOrderIntent(userMsg, userId);
        if (orderParams.isPresent()) {
            return toolCall("create_order", orderParams.get());
        }

        if (userMsg.contains("辣") || userMsg.contains("麻辣") || userMsg.contains("推荐")
                || userMsg.contains("好吃") || userMsg.contains("口味")) {
            return toolCall("semantic_search_dishes", Map.of("query", userMsg));
        }
        if (userMsg.contains("宫保鸡丁") || userMsg.contains("鸡丁")) {
            return toolCall("query_dishes", Map.of("keyword", "宫保鸡丁"));
        }
        if (userMsg.contains("订单") || userMsg.contains("订了") || userMsg.contains("购买")) {
            return toolCall("query_orders", Map.of());
        }
        if (userMsg.contains("分类") || userMsg.contains("种类")) {
            return toolCall("query_categories", Map.of());
        }
        if (containsSalesRankIntent(userMsg)) {
            return toolCall("query_dishes_sales_rank", Map.of());
        }
        if (containsDishQueryIntent(userMsg)) {
            return toolCall("query_dishes", Map.of());
        }
        return "你好！我是智能点餐小助手，请问需要帮您查询菜品、下单，还是有其他问题？";
    }

    private String extractLatestUserMessage(String prompt) {
        int marker = prompt.indexOf("用户最新提问：");
        if (marker < 0) {
            return prompt;
        }
        String part = prompt.substring(marker + "用户最新提问：".length());
        int nextLine = part.indexOf('\n');
        return (nextLine >= 0 ? part.substring(0, nextLine) : part).trim();
    }

    private boolean containsSalesRankIntent(String userMsg) {
        return userMsg.contains("销量") || userMsg.contains("畅销") || userMsg.contains("卖得最好")
                || userMsg.contains("卖得好") || (userMsg.contains("排行") && userMsg.contains("榜"));
    }

    private boolean containsDishQueryIntent(String userMsg) {
        return userMsg.contains("菜单") || userMsg.contains("好吃") || userMsg.contains("推荐")
                || userMsg.contains("有什么") || userMsg.contains("有哪些");
    }

    private Optional<Map<String, Object>> tryParseOrderIntent(String userMsg, Long userId) {
        if (!userMsg.contains("份") && !userMsg.contains("我要") && !userMsg.contains("下单")
                && !userMsg.contains("来一") && !userMsg.contains("点") && !userMsg.contains("来")) {
            return Optional.empty();
        }

        List<Map<String, Object>> items = parseOrderItems(userMsg);
        if (items.isEmpty()) {
            return Optional.empty();
        }

        Map<String, Object> params = new HashMap<>();
        params.put("items", items);
        params.put("userId", userId != null ? userId : 1L);
        return Optional.of(params);
    }

    /** 支持「三份麻婆豆腐」与「麻婆豆腐 三份」等说法 */
    private List<Map<String, Object>> parseOrderItems(String userMsg) {
        List<Map<String, Object>> items = new ArrayList<>();
        String qty = "[一二两三四五六七八九十\\d]+";

        // 三份麻婆豆腐 / 2份 宫保鸡丁
        java.util.regex.Pattern qtyFirst = java.util.regex.Pattern.compile(
                "(" + qty + ")份\\s*([^，,。！!？?\\s谢谢]+)");
        java.util.regex.Matcher m1 = qtyFirst.matcher(userMsg);
        while (m1.find()) {
            addOrderItem(items, m1.group(2), m1.group(1));
        }

        // 麻婆豆腐三份 / 麻婆豆腐 三份
        java.util.regex.Pattern nameFirst = java.util.regex.Pattern.compile(
                "([\\u4e00-\\u9fa5A-Za-z0-9]{2,}?)\\s*(" + qty + ")份");
        java.util.regex.Matcher m2 = nameFirst.matcher(userMsg);
        while (m2.find()) {
            addOrderItem(items, m2.group(1), m2.group(2));
        }

        return items;
    }

    private void addOrderItem(List<Map<String, Object>> items, String rawName, String rawQty) {
        String dishName = normalizeDishName(rawName);
        if (dishName.isEmpty()) {
            return;
        }
        int qty = parseQuantity(rawQty);
        boolean duplicate = items.stream()
                .anyMatch(i -> dishName.equals(String.valueOf(i.get("name"))));
        if (!duplicate) {
            items.add(Map.of("name", dishName, "quantity", qty));
        }
    }

    private String normalizeDishName(String raw) {
        if (raw == null) {
            return "";
        }
        String name = raw.trim()
                .replaceAll("^(我要|来|点|订|给我|帮我|想要|需要)", "")
                .trim();
        return name.length() >= 2 ? name : "";
    }

    private int parseQuantity(String raw) {
        if (raw.matches("\\d+")) {
            return Integer.parseInt(raw);
        }
        return switch (raw) {
            case "一" -> 1;
            case "二", "两" -> 2;
            case "三" -> 3;
            case "四" -> 4;
            case "五" -> 5;
            case "六" -> 6;
            case "七" -> 7;
            case "八" -> 8;
            case "九" -> 9;
            case "十" -> 10;
            default -> 1;
        };
    }

    private String toolCall(String name, Map<String, Object> params) {
        try {
            String paramsJson = objectMapper.writeValueAsString(params);
            // 使用单引号包裹 JSON，避免 params 内的双引号截断正则匹配
            return "<function name=\"" + name + "\" params='" + paramsJson + "'>";
        } catch (Exception e) {
            log.warn("构造工具调用失败: {}", e.getMessage());
            return "<function name=\"" + name + "\" params='{}'>";
        }
    }

    private String summarizeToolResults(String prompt) {
        int start = prompt.indexOf("【工具执行结果】");
        if (start < 0) {
            return "处理完成，如需帮助请继续告诉我。";
        }
        String results = prompt.substring(start);
        int instructionAt = results.indexOf("\n\n请根据");
        if (instructionAt > 0) {
            results = results.substring(0, instructionAt).trim();
        }

        if (results.contains("订单创建成功")) {
            int orderStart = results.indexOf("订单创建成功");
            String orderInfo = results.substring(orderStart).trim();
            return "好的，已为您下单！\n\n" + orderInfo;
        }
        if (results.contains("下单失败")) {
            int failStart = results.indexOf("下单失败");
            return results.substring(failStart).trim();
        }
        if (results.contains("未找到符合条件的菜品")) {
            return "抱歉，暂时没有符合您要求的菜品。您可以换个口味或告诉我具体菜名，我再帮您查。";
        }
        if (results.contains("销量排行榜")) {
            int rankStart = results.indexOf("销量排行榜");
            String rankList = results.substring(rankStart).trim();
            return "根据销量为您整理如下：\n\n" + rankList + "\n\n如需下单，请直接说菜名和份数。";
        }
        int bodyStart = results.indexOf("找到 ");
        if (bodyStart < 0) {
            return "处理完成，如需帮助请继续告诉我。";
        }
        String dishList = results.substring(bodyStart).trim();
        if (dishList.contains("道菜品")) {
            return "根据您的需求，为您推荐以下菜品：\n\n" + dishList + "\n\n如需下单，请说「我要X份菜名」。";
        }
        return "查询结果如下：\n\n" + dishList;
    }

    private Mono<String> processResponse(String sessionId, String response, Long userId, String userInput) {
        log.debug("LLM响应: {}", response);
        
        List<ToolCall> toolCalls = parseToolCalls(response);
        
        if (toolCalls.isEmpty()) {
            Optional<Map<String, Object>> orderParams = tryParseOrderIntent(userInput, userId);
            if (orderParams.isPresent()) {
                log.info("LLM 未返回工具调用，根据用户输入兜底下单: {}", userInput);
                return processResponse(sessionId, toolCall("create_order", orderParams.get()), userId, userInput);
            }
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
                            "【工具执行结果】\n" + toolResultSummary + "\n\n" +
                            "请根据以上结果，用自然、友好的语言总结给用户，不要再调用工具。";
                    
                    return Mono.fromCallable(() -> callDeepSeekWithTools(summaryPrompt, userId))
                            .flatMap(summary -> {
                                List<ToolCall> nested = parseToolCalls(summary);
                                if (nested.isEmpty()) {
                                    return chatMemoryService.addAssistantMessage(sessionId, summary)
                                            .thenReturn(summary);
                                }
                                log.warn("总结阶段仍返回工具调用，降级为工具结果直出");
                                return Mono.just(toolResultSummary.toString());
                            });
                });
    }

    private List<ToolCall> parseToolCalls(String response) {
        List<ToolCall> toolCalls = new ArrayList<>();
        String pattern = "<function name=\"(.*?)\" params=['\"](.*?)['\"]>";
        
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