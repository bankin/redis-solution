package com.solution.consumer;

import com.google.gson.Gson;
import com.solution.model.Entry;
import io.lettuce.core.api.reactive.RedisReactiveCommands;

import java.util.Random;

public class Consumer extends BaseConsumer {
    private final static Random rand = new Random();

    public Consumer(
            Gson gson,
            RedisReactiveCommands<String, String> redisListClient,
            String consumerIdsList) {
        super(gson, redisListClient, consumerIdsList);
    }

    @Override
    public Entry handleMessage(Entry entry) {
        entry.setHandledData(rand.nextInt());

        return entry;
    }
}
