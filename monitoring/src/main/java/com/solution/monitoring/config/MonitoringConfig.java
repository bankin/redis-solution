package com.solution.monitoring.config;

public record MonitoringConfig(String redisHost, int redisPort, String processedMessageStreamKey) {
}
