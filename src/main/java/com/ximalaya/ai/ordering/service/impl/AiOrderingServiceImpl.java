package com.ximalaya.ai.ordering.service.impl;

import com.ximalaya.ai.ordering.dto.request.OrderRequest;
import com.ximalaya.ai.ordering.dto.request.OrderRequest.OrderItem;
import com.ximalaya.ai.ordering.repository.DishRepository;
import com.ximalaya.ai.ordering.service.AiOrderingService;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AiOrderingServiceImpl implements AiOrderingService {

    private static final Logger log = LoggerFactory.getLogger(AiOrderingServiceImpl.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final DishRepository dishRepository;
    private final String apiKey;
    private final String baseUrl;
    private final OkHttpClient httpClient;

    private static final String ORDER_PARSING_PROMPT = """
            你是一个智能点餐助手。请分析用户的输入，提取菜品名称和数量。
            
            规则：
            1. 识别菜品名称和对应的数量
            2. 如果没有明确数量，默认为1
            3. 如果无法识别菜品，在suggestion中说明
            4. 输出格式必须是JSON格式，不要包含其他任何内容
            
            示例输出格式：
            {"items":[{"name":"宫保鸡丁","quantity":1},{"name":"鱼香肉丝","quantity":2}],"suggestion":""}
            
            用户输入：%s
            """;

    private static final String RECOMMENDATION_PROMPT = """
            你是一个智能美食推荐助手。根据用户偏好推荐合适的菜品。
            
            当前可用菜品：
            %s
            
            用户偏好：%s
            
            请根据用户偏好推荐3-5道合适的菜品，并简要说明推荐理由。
            """;

    public AiOrderingServiceImpl(DishRepository dishRepository,
                                 @Value("${ai.deepseek.api-key}") String apiKey,
                                 @Value("${ai.deepseek.base-url}") String baseUrl) {
        this.dishRepository = dishRepository;
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(java.time.Duration.ofSeconds(30))
                .readTimeout(java.time.Duration.ofSeconds(60))
                .writeTimeout(java.time.Duration.ofSeconds(30))
                .build();
    }

    @Override
    public Mono<OrderRequest> parseNaturalLanguage(String input) {
        log.info("解析自然语言点餐: {}", input);

        String prompt = String.format(ORDER_PARSING_PROMPT, input);

        return Mono.fromCallable(() -> callDeepSeek(prompt))
                .map(response -> {
                    log.debug("LLM响应: {}", response);
                    return parseOrderResponse(response);
                })
                .flatMap(parsed -> {
                    List<OrderItem> items = new ArrayList<>();
                    
                    return Mono.from(dishRepository.findByIsAvailableTrue()
                            .collectList())
                            .flatMap(allDishes -> {
                                for (Map<String, Object> item : parsed.items()) {
                                    String dishName = (String) item.get("name");
                                    int quantity = item.get("quantity") != null ? 
                                            ((Number) item.get("quantity")).intValue() : 1;
                                    
                                    allDishes.stream()
                                            .filter(dish -> dish.getName().contains(dishName) || dishName.contains(dish.getName()))
                                            .findFirst()
                                            .ifPresent(dish -> {
                                                items.add(OrderItem.builder()
                                                        .dishId(dish.getId())
                                                        .quantity(quantity)
                                                        .build());
                                            });
                                }
                                
                                OrderRequest request = OrderRequest.builder()
                                        .items(items)
                                        .remark(parsed.suggestion())
                                        .build();
                                return Mono.just(request);
                            });
                });
    }

    @Override
    public Mono<String> getDishRecommendation(Long userId, String preferences) {
        log.info("获取菜品推荐, 用户ID: {}, 偏好: {}", userId, preferences);

        return dishRepository.findByIsAvailableTrue()
                .collectList()
                .flatMap(dishes -> {
                    StringBuilder dishList = new StringBuilder();
                    dishes.forEach(dish -> {
                        dishList.append("- ").append(dish.getName())
                                .append(" (").append(dish.getCategory()).append("): ")
                                .append(dish.getDescription())
                                .append(" - ¥").append(dish.getPrice())
                                .append("\n");
                    });

                    String prompt = String.format(RECOMMENDATION_PROMPT, 
                            dishList.toString(), 
                            preferences != null ? preferences : "无特殊偏好");

                    return Mono.fromCallable(() -> callDeepSeek(prompt));
                });
    }

    private String callDeepSeek(String prompt) {
        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "deepseek-chat");
            
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
                    return "{\"items\":[],\"suggestion\":\"系统繁忙，请稍后重试\"}";
                }
                
                String responseBody = response.body() != null ? response.body().string() : "";
                return extractContent(responseBody);
            }
        } catch (Exception e) {
            log.error("DeepSeek API调用异常: {}", e.getMessage());
            return "{\"items\":[],\"suggestion\":\"系统繁忙，请稍后重试\"}";
        }
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

    private ParsedOrderResponse parseOrderResponse(String json) {
        try {
            Map<String, Object> map = objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
            
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> items = (List<Map<String, Object>>) map.get("items");
            
            String suggestion = (String) map.getOrDefault("suggestion", "");
            
            return new ParsedOrderResponse(items != null ? items : new ArrayList<>(), suggestion);
        } catch (Exception e) {
            log.warn("解析LLM响应失败: {}", e.getMessage());
            return new ParsedOrderResponse(new ArrayList<>(), "未能完全理解您的点餐需求，请重试");
        }
    }

    private record ParsedOrderResponse(List<Map<String, Object>> items, String suggestion) {}
}