package com.ximalaya.ai.ordering.service.impl;

import com.ximalaya.ai.ordering.dto.response.OperationLogResponse;
import com.ximalaya.ai.ordering.dto.response.OperationLogStatsResponse;
import com.ximalaya.ai.ordering.dto.response.PageResponse;
import com.ximalaya.ai.ordering.entity.OperationLog;
import com.ximalaya.ai.ordering.repository.OperationLogRepository;
import com.ximalaya.ai.ordering.service.OperationLogService;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class OperationLogServiceImpl implements OperationLogService {

    private final OperationLogRepository operationLogRepository;
    private final DatabaseClient databaseClient;

    public OperationLogServiceImpl(OperationLogRepository operationLogRepository,
                                 DatabaseClient databaseClient) {
        this.operationLogRepository = operationLogRepository;
        this.databaseClient = databaseClient;
    }

    @Override
    public Mono<OperationLog> record(OperationLog log) {
        if (log.getTraceId() == null || log.getTraceId().isBlank()) {
            log.setTraceId(UUID.randomUUID().toString());
        }
        if (log.getCreatedAt() == null) {
            log.setCreatedAt(LocalDateTime.now());
        }
        return operationLogRepository.save(log);
    }

    @Override
    public Mono<OperationLog> recordInternal(String module, String action, String requestParams,
                                               boolean success, String errorMessage,
                                               Long durationMs, Long userId) {
        return record(OperationLog.builder()
                .traceId(UUID.randomUUID().toString())
                .module(module)
                .action(action)
                .httpMethod("INTERNAL")
                .requestPath("internal://" + action.toLowerCase())
                .requestParams(requestParams)
                .responseStatus(success ? 200 : 500)
                .success(success)
                .errorMessage(errorMessage)
                .durationMs(durationMs)
                .userId(userId)
                .createdAt(LocalDateTime.now())
                .build());
    }

    @Override
    public Mono<PageResponse<OperationLogResponse>> queryLogs(String module, Boolean success, String keyword,
                                                            LocalDateTime startTime, LocalDateTime endTime,
                                                            int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        int offset = safePage * safeSize;

        QueryFilter filter = buildFilter(module, success, keyword, startTime, endTime);
        String whereClause = filter.whereClause();

        DatabaseClient.GenericExecuteSpec countSpec =
                bindFilter(databaseClient.sql("SELECT COUNT(*) AS cnt FROM operation_log" + whereClause), filter);

        Mono<Long> totalMono = countSpec
                .map((row, metadata) -> row.get("cnt", Long.class))
                .one()
                .defaultIfEmpty(0L);

        DatabaseClient.GenericExecuteSpec listSpec = bindFilter(
                databaseClient.sql("SELECT * FROM operation_log" + whereClause
                        + " ORDER BY created_at DESC LIMIT " + safeSize + " OFFSET " + offset),
                filter);

        Flux<OperationLog> logsFlux = listSpec.map(this::mapRow).all();

        return totalMono.zipWith(logsFlux.collectList(), (total, logs) -> {
            List<OperationLogResponse> content = logs.stream()
                    .map(OperationLogResponse::from)
                    .toList();
            return new PageResponse<>(content, safePage, safeSize, total);
        });
    }

    private QueryFilter buildFilter(String module, Boolean success, String keyword,
                                    LocalDateTime startTime, LocalDateTime endTime) {
        List<String> conditions = new ArrayList<>();
        QueryFilter filter = new QueryFilter();

        if (module != null && !module.isBlank()) {
            conditions.add("module = :module");
            filter.module = module;
        }
        if (success != null) {
            conditions.add("success = :success");
            filter.success = success;
        }
        if (keyword != null && !keyword.isBlank()) {
            conditions.add("(action LIKE :keyword OR request_path LIKE :keyword OR request_params LIKE :keyword OR trace_id LIKE :keyword)");
            filter.keyword = "%" + keyword + "%";
        }
        if (startTime != null) {
            conditions.add("created_at >= :startTime");
            filter.startTime = startTime;
        }
        if (endTime != null) {
            conditions.add("created_at <= :endTime");
            filter.endTime = endTime;
        }

        filter.conditions = conditions;
        return filter;
    }

    private DatabaseClient.GenericExecuteSpec bindFilter(DatabaseClient.GenericExecuteSpec spec, QueryFilter filter) {
        if (filter.module != null) {
            spec = spec.bind("module", filter.module);
        }
        if (filter.success != null) {
            spec = spec.bind("success", filter.success);
        }
        if (filter.keyword != null) {
            spec = spec.bind("keyword", filter.keyword);
        }
        if (filter.startTime != null) {
            spec = spec.bind("startTime", filter.startTime);
        }
        if (filter.endTime != null) {
            spec = spec.bind("endTime", filter.endTime);
        }
        return spec;
    }

    private OperationLog mapRow(io.r2dbc.spi.Readable row) {
        return OperationLog.builder()
                .id(row.get("id", Long.class))
                .traceId(row.get("trace_id", String.class))
                .module(row.get("module", String.class))
                .action(row.get("action", String.class))
                .httpMethod(row.get("http_method", String.class))
                .requestPath(row.get("request_path", String.class))
                .requestParams(row.get("request_params", String.class))
                .responseStatus(row.get("response_status", Integer.class))
                .success(row.get("success", Boolean.class))
                .errorMessage(row.get("error_message", String.class))
                .durationMs(row.get("duration_ms", Long.class))
                .userId(row.get("user_id", Long.class))
                .clientIp(row.get("client_ip", String.class))
                .createdAt(row.get("created_at", LocalDateTime.class))
                .build();
    }

    @Override
    public Mono<OperationLogResponse> getById(Long id) {
        return operationLogRepository.findById(id)
                .map(OperationLogResponse::from);
    }

    @Override
    public Flux<OperationLogResponse> getByTraceId(String traceId) {
        return operationLogRepository.findByTraceIdOrderByCreatedAtAsc(traceId)
                .map(OperationLogResponse::from);
    }

    @Override
    public Mono<OperationLogStatsResponse> getStats(LocalDateTime startTime, LocalDateTime endTime) {
        LocalDateTime start = startTime != null ? startTime : LocalDateTime.now().minusDays(7);
        LocalDateTime end = endTime != null ? endTime : LocalDateTime.now();

        Mono<OperationLogStatsResponse> summaryMono = databaseClient
                .sql("""
                        SELECT COUNT(*) AS total,
                               SUM(CASE WHEN success THEN 1 ELSE 0 END) AS success_cnt,
                               SUM(CASE WHEN NOT success THEN 1 ELSE 0 END) AS failure_cnt,
                               COALESCE(AVG(duration_ms), 0) AS avg_duration
                        FROM operation_log
                        WHERE created_at >= :startTime AND created_at <= :endTime
                        """)
                .bind("startTime", start)
                .bind("endTime", end)
                .map((row, metadata) -> {
                    OperationLogStatsResponse stats = new OperationLogStatsResponse();
                    long total = toLong(row.get("total"));
                    long successCnt = toLong(row.get("success_cnt"));
                    long failureCnt = toLong(row.get("failure_cnt"));
                    long avgDuration = toLong(row.get("avg_duration"));
                    stats.setTotalCount(total);
                    stats.setSuccessCount(successCnt);
                    stats.setFailureCount(failureCnt);
                    stats.setSuccessRate(total > 0 ? Math.round(successCnt * 10000.0 / total) / 100.0 : 0);
                    stats.setAvgDurationMs(avgDuration);
                    return stats;
                })
                .one()
                .defaultIfEmpty(new OperationLogStatsResponse());

        Flux<OperationLogStatsResponse.ModuleStat> moduleFlux = databaseClient
                .sql("""
                        SELECT module,
                               COUNT(*) AS cnt,
                               SUM(CASE WHEN success THEN 1 ELSE 0 END) AS success_cnt,
                               SUM(CASE WHEN NOT success THEN 1 ELSE 0 END) AS failure_cnt,
                               COALESCE(AVG(duration_ms), 0) AS avg_duration
                        FROM operation_log
                        WHERE created_at >= :startTime AND created_at <= :endTime
                        GROUP BY module
                        ORDER BY cnt DESC
                        """)
                .bind("startTime", start)
                .bind("endTime", end)
                .map((row, metadata) -> new OperationLogStatsResponse.ModuleStat(
                        row.get("module", String.class),
                        toLong(row.get("cnt")),
                        toLong(row.get("success_cnt")),
                        toLong(row.get("failure_cnt")),
                        toLong(row.get("avg_duration"))))
                .all();

        Flux<OperationLogStatsResponse.HourlyStat> hourlyFlux = databaseClient
                .sql("""
                        SELECT FORMATDATETIME(created_at, 'yyyy-MM-dd HH:00') AS hour_label,
                               COUNT(*) AS cnt,
                               SUM(CASE WHEN success THEN 1 ELSE 0 END) AS success_cnt
                        FROM operation_log
                        WHERE created_at >= :startTime AND created_at <= :endTime
                        GROUP BY FORMATDATETIME(created_at, 'yyyy-MM-dd HH:00')
                        ORDER BY hour_label DESC
                        LIMIT 24
                        """)
                .bind("startTime", start)
                .bind("endTime", end)
                .map((row, metadata) -> new OperationLogStatsResponse.HourlyStat(
                        row.get("hour_label", String.class),
                        toLong(row.get("cnt")),
                        toLong(row.get("success_cnt"))))
                .all();

        return summaryMono
                .zipWith(moduleFlux.collectList(), (stats, modules) -> {
                    stats.setModuleStats(modules);
                    return stats;
                })
                .zipWith(hourlyFlux.collectList(), (stats, hourly) -> {
                    stats.setHourlyStats(hourly);
                    return stats;
                });
    }

    private static long toLong(Object value) {
        if (value == null) {
            return 0L;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(value.toString());
    }

    private static class QueryFilter {
        private List<String> conditions = new ArrayList<>();
        private String module;
        private Boolean success;
        private String keyword;
        private LocalDateTime startTime;
        private LocalDateTime endTime;

        String whereClause() {
            if (conditions.isEmpty()) {
                return "";
            }
            return " WHERE " + String.join(" AND ", conditions);
        }
    }
}
