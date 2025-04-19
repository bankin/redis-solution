package com.solution.worker;

import com.google.gson.Gson;
import com.solution.config.WorkerConfig;
import com.solution.model.Entry;
import io.lettuce.core.Consumer;
import io.lettuce.core.StreamMessage;
import io.lettuce.core.XReadArgs;
import io.lettuce.core.api.reactive.RedisReactiveCommands;

abstract class BaseWorker {
    // Generate IDs from parent
    private static int CONSUMER_COUNT = 0;

    protected final int id;
    private final Gson gson;
    private final RedisReactiveCommands<String, String> redisReactiveClient;
    private final WorkerConfig workerConfig;

    public BaseWorker(
            Gson gson,
            RedisReactiveCommands<String, String> redisReactiveClient,
            WorkerConfig workerConfig) {
        this.id = ++CONSUMER_COUNT;
        this.gson = gson;
        this.redisReactiveClient = redisReactiveClient;
        this.workerConfig = workerConfig;
    }

    private Entry parseJson(String message) {
        return gson.fromJson(message, Entry.class);
    }

//    @Override
    public void unsubscribed(String channel, long count) {
        redisReactiveClient
            .lrem(this.workerConfig.activeConsumersListKey(), 1, "" + this.id)
            .subscribe();
    }

    public abstract Entry handleMessage(Entry message);

    public void start() {
        redisReactiveClient.xreadgroup(
                Consumer.from(workerConfig.consumerGroupName(), "main-consumers-" + this.id),
                    XReadArgs.StreamOffset.lastConsumed(workerConfig.messagesBacklogStreamKey())
            )
            .flatMap(message ->
                redisReactiveClient.xack(
                    workerConfig.messagesBacklogStreamKey(), workerConfig.consumerGroupName(), message.getId()
                )
                .map($ -> message))
            .map(StreamMessage::getBody)
            .filter(body -> !body.isEmpty() && body.get("message") != null)
            .map(body -> {
                Entry entry = this.parseJson(body.get("message"));

                return this.handleMessage(entry);
            })
            .flatMap(result -> redisReactiveClient
                .xadd(workerConfig.processedMessagesStreamKey(), "processed", gson.toJson(result)))
            .repeat()
            .subscribe();
    }
}
