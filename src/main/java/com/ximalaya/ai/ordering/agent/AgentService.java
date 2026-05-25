package com.ximalaya.ai.ordering.agent;

import reactor.core.publisher.Mono;

public interface AgentService {
    
    Mono<String> chat(String sessionId, String userInput, Long userId);
    
    Mono<String> getSessionSummary(String sessionId);
    
    Mono<Void> clearSession(String sessionId);
}