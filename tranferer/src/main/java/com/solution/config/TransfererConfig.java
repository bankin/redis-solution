package com.solution.config;

public record TransfererConfig(String redisHost, int redisPort, String pubSubKey, String backlogStreamKey) {
}
