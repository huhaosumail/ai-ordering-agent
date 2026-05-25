package com.ximalaya.ai.ordering.service;

import com.ximalaya.ai.ordering.dto.response.OperationLogResponse;
import com.ximalaya.ai.ordering.dto.response.OperationLogStatsResponse;
import com.ximalaya.ai.ordering.dto.response.PageResponse;
import com.ximalaya.ai.ordering.entity.OperationLog;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

public interface OperationLogService {

    Mono<OperationLog> record(OperationLog log);

    Mono<OperationLog> recordInternal(String module, String action, String requestParams,
                                      boolean success, String errorMessage, Long durationMs, Long userId);

    Mono<PageResponse<OperationLogResponse>> queryLogs(String module, Boolean success, String keyword,
                                                     LocalDateTime startTime, LocalDateTime endTime,
                                                     int page, int size);

    Mono<OperationLogResponse> getById(Long id);

    Flux<OperationLogResponse> getByTraceId(String traceId);

    Mono<OperationLogStatsResponse> getStats(LocalDateTime startTime, LocalDateTime endTime);
}
