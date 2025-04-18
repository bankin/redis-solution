package com.solution.consumer;

import com.google.gson.Gson;
import com.solution.model.Entry;
import io.lettuce.core.Consumer;
import io.lettuce.core.XReadArgs;
import io.lettuce.core.api.reactive.RedisReactiveCommands;
import io.lettuce.core.pubsub.RedisPubSubAdapter;

import java.time.Duration;
import java.util.Random;

import static io.lettuce.core.SetArgs.Builder.nx;

// TODO Generic magic
abstract class BaseConsumer extends RedisPubSubAdapter<String, String> {
    private static int CONSUMER_COUNT = 0;

    private final int id;
    private final Gson gson;
    // FIXME: Integer value type
    private final RedisReactiveCommands<String, String> redisReactiveClient;
    private final String consumerIdsList;

    public BaseConsumer(
            Gson gson,
            RedisReactiveCommands<String, String> redisReactiveClient,
            String consumerIdsList) {
        this.id = ++CONSUMER_COUNT;
        this.gson = gson;
        this.redisReactiveClient = redisReactiveClient;
        this.consumerIdsList = consumerIdsList;
    }

    @Override
    public void message(String channel, String message) {
        // FIXME Ack
        Entry parsed = this.parseJson(message);

//        Entry result = this.handleMessage(parsed);
//        redisReactiveClient
//            .xadd("messages:processed", "processed by " + this.id, gson.toJson(result))
//            .subscribe();

        redisReactiveClient.sadd("parsed:ids", parsed.getMessageId())
            .filter(res -> res > 0)
            .map($ -> this.handleMessage(parsed))
            .flatMap(result -> redisReactiveClient
                    .xadd("messages:processed", "processed", gson.toJson(result)))
            .subscribe();
    }

    private Entry parseJson(String message) {
        return gson.fromJson(message, Entry.class);
    }

    @Override
    public void subscribed(String channel, long count) {
        redisReactiveClient
            .rpush(this.consumerIdsList, "" + this.id)
            .subscribe();
    }

    @Override
    public void unsubscribed(String channel, long count) {
        redisReactiveClient
            .lrem(this.consumerIdsList, 1, "" + this.id)
            .subscribe();
    }

    public abstract Entry handleMessage(Entry message);
}
