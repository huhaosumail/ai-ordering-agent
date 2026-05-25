package com.ximalaya.ai.ordering.repository;

import com.ximalaya.ai.ordering.entity.ChatHistory;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Repository
public interface ChatHistoryRepository extends ReactiveCrudRepository<ChatHistory, Long> {
    
    Flux<ChatHistory> findBySessionIdOrderByCreatedAtAsc(String sessionId);
    
    Mono<Long> countBySessionId(String sessionId);
    
    @Query("SELECT * FROM chat_history WHERE session_id = :sessionId ORDER BY created_at DESC LIMIT :limit")
    Flux<ChatHistory> findRecentBySessionId(String sessionId, int limit);
    
    Mono<Void> deleteBySessionId(String sessionId);
    
    Mono<Void> deleteByCreatedAtBefore(LocalDateTime dateTime);
    
    @Query("SELECT * FROM chat_history WHERE user_id = :userId ORDER BY created_at DESC LIMIT 20")
    Flux<ChatHistory> findRecentByUserId(Long userId);
}