package com.solution.consumer;

import com.google.gson.Gson;
import com.solution.model.Entry;
import io.lettuce.core.Consumer;
import io.lettuce.core.StreamMessage;
import io.lettuce.core.XReadArgs;
import io.lettuce.core.api.reactive.RedisReactiveCommands;

// TODO Generic magic
abstract class BaseConsumer {
    private static int CONSUMER_COUNT = 0;

    private final int id;
    private final Gson gson;
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

    private Entry parseJson(String message) {
        return gson.fromJson(message, Entry.class);
    }

//    @Override
    public void subscribed(String channel, long count) {
        redisReactiveClient
            .rpush(this.consumerIdsList, "" + this.id)
            .subscribe();
    }

//    @Override
    public void unsubscribed(String channel, long count) {
        redisReactiveClient
            .lrem(this.consumerIdsList, 1, "" + this.id)
            .subscribe();
    }

    public abstract Entry handleMessage(Entry message);

    public void start() {
        redisReactiveClient.xreadgroup(
                Consumer.from("main-consumers", "main-consumers-" + this.id),
                    XReadArgs.StreamOffset.lastConsumed("messages:backlog")
            )
            .flatMap(message ->
                redisReactiveClient.xack("messages:backlog", "main-consumers", message.getId())
                    .map($ -> message))
            .map(StreamMessage::getBody)
            .filter(body -> !body.isEmpty() && body.get("message") != null)
            .map(body -> {
                Entry entry = this.parseJson(body.get("message"));

                return this.handleMessage(entry);
            })
            .flatMap(result -> redisReactiveClient
                .xadd("messages:processed", "processed", gson.toJson(result)))
            .repeat()
            .subscribe();
    }
}
