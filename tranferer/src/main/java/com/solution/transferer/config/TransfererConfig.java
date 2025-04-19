package com.solution.transferer.config;

public record TransfererConfig(String redisHost, int redisPort, String pubSubKey, String backlogStreamKey) {
}
