package com.ximalaya.ai.ordering.dto.response;

import java.util.List;
import java.util.Map;

public class OperationLogStatsResponse {

    private long totalCount;
    private long successCount;
    private long failureCount;
    private double successRate;
    private long avgDurationMs;
    private List<ModuleStat> moduleStats;
    private List<HourlyStat> hourlyStats;

    public static class ModuleStat {
        private String module;
        private long count;
        private long successCount;
        private long failureCount;
        private long avgDurationMs;

        public ModuleStat(String module, long count, long successCount, long failureCount, long avgDurationMs) {
            this.module = module;
            this.count = count;
            this.successCount = successCount;
            this.failureCount = failureCount;
            this.avgDurationMs = avgDurationMs;
        }

        public String getModule() { return module; }
        public long getCount() { return count; }
        public long getSuccessCount() { return successCount; }
        public long getFailureCount() { return failureCount; }
        public long getAvgDurationMs() { return avgDurationMs; }
    }

    public static class HourlyStat {
        private String hour;
        private long count;
        private long successCount;

        public HourlyStat(String hour, long count, long successCount) {
            this.hour = hour;
            this.count = count;
            this.successCount = successCount;
        }

        public String getHour() { return hour; }
        public long getCount() { return count; }
        public long getSuccessCount() { return successCount; }
    }

    public long getTotalCount() { return totalCount; }
    public void setTotalCount(long totalCount) { this.totalCount = totalCount; }

    public long getSuccessCount() { return successCount; }
    public void setSuccessCount(long successCount) { this.successCount = successCount; }

    public long getFailureCount() { return failureCount; }
    public void setFailureCount(long failureCount) { this.failureCount = failureCount; }

    public double getSuccessRate() { return successRate; }
    public void setSuccessRate(double successRate) { this.successRate = successRate; }

    public long getAvgDurationMs() { return avgDurationMs; }
    public void setAvgDurationMs(long avgDurationMs) { this.avgDurationMs = avgDurationMs; }

    public List<ModuleStat> getModuleStats() { return moduleStats; }
    public void setModuleStats(List<ModuleStat> moduleStats) { this.moduleStats = moduleStats; }

    public List<HourlyStat> getHourlyStats() { return hourlyStats; }
    public void setHourlyStats(List<HourlyStat> hourlyStats) { this.hourlyStats = hourlyStats; }
}
