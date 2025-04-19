package com.solution.config;

public record WorkerConfig(
    String messagesBacklogStreamKey,
    String activeConsumersListKey,
    String consumerGroupName,
    String processedMessagesStreamKey) {
}
