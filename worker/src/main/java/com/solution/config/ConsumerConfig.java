package com.solution.config;

public record ConsumerConfig(
    RedisConfig redisConfig,
    int consumerCount,
    WorkerConfig workerConfig
) {}
