package com.solution.config;

public record MonitoringConfig(String redisHost, int redisPort, String processedMessageStreamKey) {
}
