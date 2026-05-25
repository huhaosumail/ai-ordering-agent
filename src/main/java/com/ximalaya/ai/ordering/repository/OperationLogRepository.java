package com.ximalaya.ai.ordering.repository;

import com.ximalaya.ai.ordering.entity.OperationLog;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface OperationLogRepository extends ReactiveCrudRepository<OperationLog, Long> {

    Flux<OperationLog> findByTraceIdOrderByCreatedAtAsc(String traceId);

    Flux<OperationLog> findByModuleOrderByCreatedAtDesc(String module);

    Mono<Long> countBySuccess(Boolean success);
}
