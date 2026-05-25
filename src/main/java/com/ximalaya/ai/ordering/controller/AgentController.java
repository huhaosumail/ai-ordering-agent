package com.ximalaya.ai.ordering.controller;

import com.ximalaya.ai.ordering.agent.AgentService;
import com.ximalaya.ai.ordering.dto.response.ApiResponse;
import com.ximalaya.ai.ordering.service.ChatMemoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/api/agent")
public class AgentController {

    private static final Logger log = LoggerFactory.getLogger(AgentController.class);

    private final AgentService agentService;
    private final ChatMemoryService chatMemoryService;

    public AgentController(AgentService agentService, ChatMemoryService chatMemoryService) {
        this.agentService = agentService;
        this.chatMemoryService = chatMemoryService;
    }

    @PostMapping("/chat")
    public Mono<ResponseEntity<ApiResponse<String>>> chat(@RequestBody Map<String, Object> request) {
        String sessionId = (String) request.get("sessionId");
        String message = (String) request.get("message");
        Long userId = request.get("userId") != null ? ((Number) request.get("userId")).longValue() : null;

        if (sessionId == null || sessionId.isEmpty()) {
            sessionId = java.util.UUID.randomUUID().toString();
        }

        log.info("Agent聊天请求: sessionId={}, message={}, userId={}", sessionId, message, userId);

        return agentService.chat(sessionId, message, userId)
                .map(response -> ResponseEntity.ok(ApiResponse.success(response)));
    }

    @GetMapping("/session/{sessionId}/summary")
    public Mono<ResponseEntity<ApiResponse<String>>> getSessionSummary(@PathVariable String sessionId) {
        log.info("获取对话摘要: sessionId={}", sessionId);

        return agentService.getSessionSummary(sessionId)
                .map(summary -> ResponseEntity.ok(ApiResponse.success(summary)));
    }

    @DeleteMapping("/session/{sessionId}")
    public Mono<ResponseEntity<ApiResponse<String>>> clearSession(@PathVariable String sessionId) {
        log.info("清除对话: sessionId={}", sessionId);

        return agentService.clearSession(sessionId)
                .then(Mono.just(ResponseEntity.ok(ApiResponse.success("对话已清除"))));
    }

    @GetMapping("/session/{sessionId}/messages")
    public Mono<ResponseEntity<ApiResponse<Iterable<Map<String, Object>>>>> getSessionMessages(@PathVariable String sessionId) {
        log.info("获取对话消息列表: sessionId={}", sessionId);

        return chatMemoryService.getSessionMessages(sessionId)
                .collectList()
                .map(messages -> ResponseEntity.ok(ApiResponse.success(messages)));
    }
}