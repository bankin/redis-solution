package com.solution;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.solution.consumer.Consumer;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.Value;
import io.lettuce.core.XReadArgs;
import io.lettuce.core.api.reactive.RedisReactiveCommands;
import io.lettuce.core.pubsub.RedisPubSubListener;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import io.lettuce.core.pubsub.api.reactive.RedisPubSubReactiveCommands;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.Objects;

import static com.google.gson.FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES;

public class ConsumerMain {
    private static final Gson gson = new GsonBuilder()
            .setFieldNamingPolicy(LOWER_CASE_WITH_UNDERSCORES)
            .create();

    public static void start() {
        int consumerCount = 2;

        RedisURI uri = RedisURI.Builder
                .redis("localhost", 6379)
                .build();
        RedisClient client = RedisClient.create(uri);

        RedisReactiveCommands<String, String> baseReactive = client.connect().reactive();

        StatefulRedisPubSubConnection<String, String> connection = client.connectPubSub();
        for (int i = 0; i < consumerCount; i++) {
            RedisPubSubListener<String, String> first = new Consumer(gson, baseReactive, "consumer:ids");
            connection.addListener(first);
        }

        RedisPubSubReactiveCommands<String, String> pubSubReactive = connection.reactive();
        pubSubReactive.subscribe("messages:published").subscribe();

        while (true) {}
    }
}