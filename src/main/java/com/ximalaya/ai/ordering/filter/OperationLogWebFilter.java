package com.ximalaya.ai.ordering.filter;

import com.ximalaya.ai.ordering.entity.OperationLog;
import com.ximalaya.ai.ordering.service.OperationLogService;
import com.ximalaya.ai.ordering.util.OperationLogHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class OperationLogWebFilter implements WebFilter {

    private static final Logger log = LoggerFactory.getLogger(OperationLogWebFilter.class);

    private final OperationLogService operationLogService;

    public OperationLogWebFilter(OperationLogService operationLogService) {
        this.operationLogService = operationLogService;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        if (shouldSkip(path)) {
            return chain.filter(exchange);
        }

        String headerTraceId = request.getHeaders().getFirst(OperationLogHelper.TRACE_ID_HEADER);
        final String traceId = (headerTraceId == null || headerTraceId.isBlank())
                ? UUID.randomUUID().toString()
                : headerTraceId;
        exchange.getAttributes().put(OperationLogHelper.TRACE_ID_ATTR, traceId);
        exchange.getResponse().getHeaders().add(OperationLogHelper.TRACE_ID_HEADER, traceId);

        long startTime = System.currentTimeMillis();
        AtomicReference<String> requestBodyRef = new AtomicReference<>();
        AtomicReference<Throwable> errorRef = new AtomicReference<>();

        Mono<Void> chainMono;
        if (OperationLogHelper.hasRequestBody(request.getMethod())) {
            chainMono = DataBufferUtils.join(request.getBody())
                    .defaultIfEmpty(exchange.getResponse().bufferFactory().allocateBuffer(0))
                    .flatMap(dataBuffer -> {
                        byte[] bytes = new byte[dataBuffer.readableByteCount()];
                        if (bytes.length > 0) {
                            dataBuffer.read(bytes);
                            requestBodyRef.set(new String(bytes, StandardCharsets.UTF_8));
                        }
                        DataBufferUtils.release(dataBuffer);

                        ServerHttpRequest decorated = new ServerHttpRequestDecorator(request) {
                            @Override
                            public reactor.core.publisher.Flux<DataBuffer> getBody() {
                                if (bytes.length == 0) {
                                    return reactor.core.publisher.Flux.empty();
                                }
                                DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
                                return reactor.core.publisher.Flux.just(buffer);
                            }
                        };
                        return chain.filter(exchange.mutate().request(decorated).build());
                    });
        } else {
            chainMono = chain.filter(exchange);
        }

        return chainMono
                .doOnError(errorRef::set)
                .doFinally(signal -> saveLog(exchange, request, path, traceId, startTime, requestBodyRef.get(), errorRef.get())
                        .subscribe(
                                saved -> log.debug("操作日志已记录: traceId={}, path={}", traceId, path),
                                e -> log.warn("操作日志记录失败: {}", e.getMessage())
                        ));
    }

    private boolean shouldSkip(String path) {
        return path.startsWith("/h2-console")
                || path.startsWith("/actuator")
                || path.endsWith(".ico")
                || path.endsWith(".css")
                || path.endsWith(".js");
    }

    private Mono<OperationLog> saveLog(ServerWebExchange exchange, ServerHttpRequest request,
                                     String path, String traceId, long startTime,
                                     String requestBody, Throwable error) {
        long duration = System.currentTimeMillis() - startTime;
        HttpStatusCode statusCode = exchange.getResponse().getStatusCode();
        int status = statusCode != null ? statusCode.value() : (error != null ? 500 : 200);
        boolean success = error == null && status < 400;

        String method = request.getMethod() != null ? request.getMethod().name() : "UNKNOWN";
        String params = OperationLogHelper.buildRequestParams(request, requestBody);
        Long userId = OperationLogHelper.extractUserId(request, requestBody);

        OperationLog operationLog = OperationLog.builder()
                .traceId(traceId)
                .module(OperationLogHelper.resolveModule(path))
                .action(OperationLogHelper.resolveAction(method, path))
                .httpMethod(method)
                .requestPath(path)
                .requestParams(OperationLogHelper.truncate(params))
                .responseStatus(status)
                .success(success)
                .errorMessage(error != null ? OperationLogHelper.truncate(error.getMessage()) : null)
                .durationMs(duration)
                .userId(userId)
                .clientIp(resolveClientIp(request))
                .createdAt(LocalDateTime.now())
                .build();

        return operationLogService.record(operationLog);
    }

    private String resolveClientIp(ServerHttpRequest request) {
        String forwarded = request.getHeaders().getFirst("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        if (request.getRemoteAddress() != null && request.getRemoteAddress().getAddress() != null) {
            return request.getRemoteAddress().getAddress().getHostAddress();
        }
        return "unknown";
    }
}
