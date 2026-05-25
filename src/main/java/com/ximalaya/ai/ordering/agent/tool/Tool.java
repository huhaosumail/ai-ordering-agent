package com.ximalaya.ai.ordering.agent.tool;

import reactor.core.publisher.Mono;

import java.util.Map;

public interface Tool {
    
    String getName();
    
    String getDescription();
    
    Mono<String> execute(Map<String, Object> parameters);
}