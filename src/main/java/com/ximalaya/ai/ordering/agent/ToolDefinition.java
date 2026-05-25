package com.ximalaya.ai.ordering.agent;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ToolDefinition {
    
    private String name;
    
    private String description;
    
    private List<ToolParameter> parameters;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ToolParameter {
        private String name;
        private String type;
        private String description;
        private boolean required;
    }
}