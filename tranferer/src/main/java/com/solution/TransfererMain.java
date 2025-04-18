package com.solution;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.reactive.RedisReactiveCommands;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import io.lettuce.core.pubsub.api.reactive.RedisPubSubReactiveCommands;

public class TransfererMain {
    public static void start() {
        RedisURI uri = RedisURI.Builder
                .redis("localhost", 6379)
                .build();
        RedisClient client = RedisClient.create(uri);

        RedisReactiveCommands<String, String> baseReactive = client.connect().reactive();

        StatefulRedisPubSubConnection<String, String> connection = client.connectPubSub();
        RedisPubSubReactiveCommands<String, String> pubSubReactive = connection.reactive();

        pubSubReactive.subscribe("messages:published").subscribe();
        pubSubReactive.observeChannels()
            .flatMap(message -> baseReactive.lpush("messages:backlog", message.getMessage()))
            .subscribe();
    }
}