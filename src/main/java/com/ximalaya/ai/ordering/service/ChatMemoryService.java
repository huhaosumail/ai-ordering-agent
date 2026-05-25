package com.ximalaya.ai.ordering.service;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

public interface ChatMemoryService {
    
    Mono<String> getSessionHistory(String sessionId, int maxMessages);
    
    Mono<Void> addUserMessage(String sessionId, String content, Long userId);
    
    Mono<Void> addAssistantMessage(String sessionId, String content);
    
    Mono<Void> addToolMessage(String sessionId, String toolName, String toolResult);
    
    Mono<Void> clearSession(String sessionId);
    
    Mono<Long> getSessionMessageCount(String sessionId);
    
    Flux<Map<String, Object>> getSessionMessages(String sessionId);
}