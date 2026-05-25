package com.ximalaya.ai.ordering.controller;

import com.ximalaya.ai.ordering.dto.response.ApiResponse;
import com.ximalaya.ai.ordering.dto.response.OperationLogResponse;
import com.ximalaya.ai.ordering.dto.response.OperationLogStatsResponse;
import com.ximalaya.ai.ordering.dto.response.PageResponse;
import com.ximalaya.ai.ordering.service.OperationLogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/logs")
public class OperationLogController {

    private static final Logger log = LoggerFactory.getLogger(OperationLogController.class);

    private final OperationLogService operationLogService;

    public OperationLogController(OperationLogService operationLogService) {
        this.operationLogService = operationLogService;
    }

    @GetMapping
    public Mono<ResponseEntity<ApiResponse<PageResponse<OperationLogResponse>>>> queryLogs(
            @RequestParam(required = false) String module,
            @RequestParam(required = false) Boolean success,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("查询操作日志, module={}, success={}, keyword={}, page={}, size={}", module, success, keyword, page, size);
        return operationLogService.queryLogs(module, success, keyword, startTime, endTime, page, size)
                .map(result -> ResponseEntity.ok(ApiResponse.success(result)));
    }

    @GetMapping("/stats")
    public Mono<ResponseEntity<ApiResponse<OperationLogStatsResponse>>> getStats(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        log.info("查询操作日志统计, startTime={}, endTime={}", startTime, endTime);
        return operationLogService.getStats(startTime, endTime)
                .map(stats -> ResponseEntity.ok(ApiResponse.success(stats)));
    }

    @GetMapping("/trace/{traceId}")
    public Mono<ResponseEntity<ApiResponse<?>>> getLogsByTraceId(@PathVariable String traceId) {
        log.info("按链路ID查询操作日志, traceId={}", traceId);
        return operationLogService.getByTraceId(traceId)
                .collectList()
                .map(logs -> ResponseEntity.ok(ApiResponse.success(logs)));
    }

    @GetMapping("/{id}")
    public Mono<ResponseEntity<ApiResponse<OperationLogResponse>>> getLogById(@PathVariable Long id) {
        log.info("查询操作日志详情, id={}", id);
        return operationLogService.getById(id)
                .map(item -> ResponseEntity.ok(ApiResponse.success(item)));
    }
}
