package com.ximalaya.ai.ordering.dto.response;

import com.ximalaya.ai.ordering.entity.OperationLog;

import java.time.LocalDateTime;

public class OperationLogResponse {

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

    public static OperationLogResponse from(OperationLog log) {
        OperationLogResponse response = new OperationLogResponse();
        response.id = log.getId();
        response.traceId = log.getTraceId();
        response.module = log.getModule();
        response.action = log.getAction();
        response.httpMethod = log.getHttpMethod();
        response.requestPath = log.getRequestPath();
        response.requestParams = log.getRequestParams();
        response.responseStatus = log.getResponseStatus();
        response.success = log.getSuccess();
        response.errorMessage = log.getErrorMessage();
        response.durationMs = log.getDurationMs();
        response.userId = log.getUserId();
        response.clientIp = log.getClientIp();
        response.createdAt = log.getCreatedAt();
        return response;
    }

    public Long getId() { return id; }
    public String getTraceId() { return traceId; }
    public String getModule() { return module; }
    public String getAction() { return action; }
    public String getHttpMethod() { return httpMethod; }
    public String getRequestPath() { return requestPath; }
    public String getRequestParams() { return requestParams; }
    public Integer getResponseStatus() { return responseStatus; }
    public Boolean getSuccess() { return success; }
    public String getErrorMessage() { return errorMessage; }
    public Long getDurationMs() { return durationMs; }
    public Long getUserId() { return userId; }
    public String getClientIp() { return clientIp; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
