package com.solution;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.solution.consumer.Consumer;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.XReadArgs;
import io.lettuce.core.api.reactive.RedisReactiveCommands;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static com.google.gson.FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES;
import static io.lettuce.core.XGroupCreateArgs.Builder.mkstream;

public class ConsumerMain {
    private static final Gson gson = new GsonBuilder()
            .setFieldNamingPolicy(LOWER_CASE_WITH_UNDERSCORES)
            .create();

    private static final String STREAM_KEY = "messages:backlog";
    private static final String CONSUMER_GROUP_NAME = "main-consumers";

    public static void start() {
        RedisURI uri = RedisURI.Builder
                .redis("localhost", 6379)
                .build();
        RedisClient client = RedisClient.create(uri);

        RedisReactiveCommands<String, String> baseReactive = client.connect().reactive();

        createConsumerGroup(baseReactive)
            .flatMapMany($ -> Flux.range(1, 10))
            .flatMap(id ->
                baseReactive
                    .xgroupCreateconsumer("messages:backlog", io.lettuce.core.Consumer.from("main-consumers", "main-consumers-" + id))
                    .doOnNext($ -> new Consumer(gson, baseReactive, "consumer:ids").start()))
            .subscribe();
    }

    private static Mono<String> createConsumerGroup(RedisReactiveCommands<String, String> baseReactive) {
        return baseReactive.xgroupDestroy(STREAM_KEY, CONSUMER_GROUP_NAME)
            .flatMap($ ->
                baseReactive.xgroupCreate(XReadArgs.StreamOffset.latest(STREAM_KEY), CONSUMER_GROUP_NAME, mkstream())
            );
    }
}