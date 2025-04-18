package com.solution.config;

public record Consumer(
    int count,
    String channel,
    String activeIdsList,
    String parsedMessagesStream
) {}
