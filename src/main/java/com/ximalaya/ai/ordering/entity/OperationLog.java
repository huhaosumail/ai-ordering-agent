package com.ximalaya.ai.ordering.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Table("operation_log")
public class OperationLog {

    @Id
    private Long id;

    @Column("trace_id")
    private String traceId;

    @Column("module")
    private String module;

    @Column("action")
    private String action;

    @Column("http_method")
    private String httpMethod;

    @Column("request_path")
    private String requestPath;

    @Column("request_params")
    private String requestParams;

    @Column("response_status")
    private Integer responseStatus;

    @Column("success")
    private Boolean success;

    @Column("error_message")
    private String errorMessage;

    @Column("duration_ms")
    private Long durationMs;

    @Column("user_id")
    private Long userId;

    @Column("client_ip")
    private String clientIp;

    @Column("created_at")
    private LocalDateTime createdAt;

    public OperationLog() {}

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private String traceId;
        private String module;
        private String action;
        private String httpMethod;
        private String requestPath;
        private String requestParams;
        private Integer responseStatus;
        private Boolean success;
        private String errorMessage;
        private Long durationMs;
        private Long userId;
        private String clientIp;
        private LocalDateTime createdAt;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder traceId(String traceId) { this.traceId = traceId; return this; }
        public Builder module(String module) { this.module = module; return this; }
        public Builder action(String action) { this.action = action; return this; }
        public Builder httpMethod(String httpMethod) { this.httpMethod = httpMethod; return this; }
        public Builder requestPath(String requestPath) { this.requestPath = requestPath; return this; }
        public Builder requestParams(String requestParams) { this.requestParams = requestParams; return this; }
        public Builder responseStatus(Integer responseStatus) { this.responseStatus = responseStatus; return this; }
        public Builder success(Boolean success) { this.success = success; return this; }
        public Builder errorMessage(String errorMessage) { this.errorMessage = errorMessage; return this; }
        public Builder durationMs(Long durationMs) { this.durationMs = durationMs; return this; }
        public Builder userId(Long userId) { this.userId = userId; return this; }
        public Builder clientIp(String clientIp) { this.clientIp = clientIp; return this; }
        public Builder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }

        public OperationLog build() {
            OperationLog log = new OperationLog();
            log.id = id;
            log.traceId = traceId;
            log.module = module;
            log.action = action;
            log.httpMethod = httpMethod;
            log.requestPath = requestPath;
            log.requestParams = requestParams;
            log.responseStatus = responseStatus;
            log.success = success;
            log.errorMessage = errorMessage;
            log.durationMs = durationMs;
            log.userId = userId;
            log.clientIp = clientIp;
            log.createdAt = createdAt;
            return log;
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTraceId() { return traceId; }
    public void setTraceId(String traceId) { this.traceId = traceId; }

    public String getModule() { return module; }
    public void setModule(String module) { this.module = module; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getHttpMethod() { return httpMethod; }
    public void setHttpMethod(String httpMethod) { this.httpMethod = httpMethod; }

    public String getRequestPath() { return requestPath; }
    public void setRequestPath(String requestPath) { this.requestPath = requestPath; }

    public String getRequestParams() { return requestParams; }
    public void setRequestParams(String requestParams) { this.requestParams = requestParams; }

    public Integer getResponseStatus() { return responseStatus; }
    public void setResponseStatus(Integer responseStatus) { this.responseStatus = responseStatus; }

    public Boolean getSuccess() { return success; }
    public void setSuccess(Boolean success) { this.success = success; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public Long getDurationMs() { return durationMs; }
    public void setDurationMs(Long durationMs) { this.durationMs = durationMs; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getClientIp() { return clientIp; }
    public void setClientIp(String clientIp) { this.clientIp = clientIp; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
