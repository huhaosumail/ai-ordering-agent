package com.ximalaya.ai.ordering.service.impl;

import com.ximalaya.ai.ordering.entity.ChatHistory;
import com.ximalaya.ai.ordering.repository.ChatHistoryRepository;
import com.ximalaya.ai.ordering.service.ChatMemoryService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class ChatMemoryServiceImpl implements ChatMemoryService {

    private static final Logger log = LoggerFactory.getLogger(ChatMemoryServiceImpl.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final ChatHistoryRepository chatHistoryRepository;

    public ChatMemoryServiceImpl(ChatHistoryRepository chatHistoryRepository) {
        this.chatHistoryRepository = chatHistoryRepository;
    }

    @Override
    public Mono<String> getSessionHistory(String sessionId, int maxMessages) {
        return chatHistoryRepository.findRecentBySessionId(sessionId, maxMessages)
                .collectList()
                .map(histories -> {
                    Collections.reverse(histories);
                    StringBuilder history = new StringBuilder();
                    for (ChatHistory h : histories) {
                        if ("user".equals(h.getRole())) {
                            history.append("用户: ").append(h.getContent()).append("\n");
                        } else if ("assistant".equals(h.getRole())) {
                            history.append("助手: ").append(h.getContent()).append("\n");
                        } else if ("tool".equals(h.getRole())) {
                            history.append("工具[").append(h.getToolName()).append("]: ").append(h.getToolResult()).append("\n");
                        }
                    }
                    return history.toString();
                });
    }

    @Override
    public Mono<Void> addUserMessage(String sessionId, String content, Long userId) {
        ChatHistory history = ChatHistory.builder()
                .sessionId(sessionId)
                .role("user")
                .content(content)
                .userId(userId)
                .createdAt(LocalDateTime.now())
                .build();
        return chatHistoryRepository.save(history).then();
    }

    @Override
    public Mono<Void> addAssistantMessage(String sessionId, String content) {
        ChatHistory history = ChatHistory.builder()
                .sessionId(sessionId)
                .role("assistant")
                .content(content)
                .createdAt(LocalDateTime.now())
                .build();
        return chatHistoryRepository.save(history).then();
    }

    @Override
    public Mono<Void> addToolMessage(String sessionId, String toolName, String toolResult) {
        ChatHistory history = ChatHistory.builder()
                .sessionId(sessionId)
                .role("tool")
                .toolName(toolName)
                .toolResult(toolResult)
                .createdAt(LocalDateTime.now())
                .build();
        return chatHistoryRepository.save(history).then();
    }

    @Override
    public Mono<Void> clearSession(String sessionId) {
        return chatHistoryRepository.deleteBySessionId(sessionId);
    }

    @Override
    public Mono<Long> getSessionMessageCount(String sessionId) {
        return chatHistoryRepository.countBySessionId(sessionId);
    }

    @Override
    public Flux<Map<String, Object>> getSessionMessages(String sessionId) {
        return chatHistoryRepository.findBySessionIdOrderByCreatedAtAsc(sessionId)
                .map(h -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("role", h.getRole());
                    map.put("content", h.getContent());
                    if (h.getToolName() != null) {
                        map.put("toolName", h.getToolName());
                    }
                    if (h.getToolResult() != null) {
                        map.put("toolResult", h.getToolResult());
                    }
                    map.put("createdAt", h.getCreatedAt());
                    return map;
                });
    }

    public Mono<List<Map<String, String>>> getMessagesForLlm(String sessionId, int maxMessages) {
        return chatHistoryRepository.findRecentBySessionId(sessionId, maxMessages)
                .collectList()
                .map(histories -> {
                    Collections.reverse(histories);
                    List<Map<String, String>> messages = new ArrayList<>();
                    for (ChatHistory h : histories) {
                        Map<String, String> message = new HashMap<>();
                        message.put("role", h.getRole());
                        message.put("content", h.getContent());
                        messages.add(message);
                    }
                    return messages;
                });
    }
}