package com.ximalaya.ai.ordering.controller;

import com.ximalaya.ai.ordering.feishu.FeishuEventService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/feishu")
@ConditionalOnProperty(name = "feishu.enabled", havingValue = "true")
public class FeishuController {

    private static final Logger log = LoggerFactory.getLogger(FeishuController.class);

    private final FeishuEventService feishuEventService;

    public FeishuController(FeishuEventService feishuEventService) {
        this.feishuEventService = feishuEventService;
    }

    /**
     * 飞书事件订阅回调地址（需在开放平台配置为 POST）
     */
    @PostMapping(value = "/webhook", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<String>> webhook(@RequestBody String rawBody) {
        log.debug("收到飞书回调: {} bytes", rawBody.length());

        return feishuEventService.parsePayload(rawBody)
                .flatMap(feishuEventService::handlePayload)
                .flatMap(body -> feishuEventService.encryptResponseIfNeeded(body)
                        .map(json -> ResponseEntity.ok()
                                .contentType(MediaType.APPLICATION_JSON)
                                .body(json)))
                .onErrorResume(SecurityException.class, e -> {
                    log.warn("飞书回调校验失败: {}", e.getMessage());
                    return Mono.just(ResponseEntity.status(403).body("{\"error\":\"forbidden\"}"));
                })
                .onErrorResume(e -> {
                    log.error("飞书回调处理失败", e);
                    return Mono.just(ResponseEntity.internalServerError()
                            .body("{\"error\":\"internal\"}"));
                });
    }
}
