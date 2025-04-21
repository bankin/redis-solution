package com.solution.worker;

import com.google.gson.Gson;
import com.solution.config.WorkerConfig;
import com.solution.model.Entry;
import io.lettuce.core.Consumer;
import io.lettuce.core.StreamMessage;
import io.lettuce.core.XReadArgs;
import io.lettuce.core.api.reactive.RedisReactiveCommands;

abstract class BaseWorker {
    protected final int id;
    private final Gson gson;
    private final RedisReactiveCommands<String, String> redisReactiveClient;
    private final WorkerConfig workerConfig;

    public BaseWorker(
            int id,
            Gson gson,
            RedisReactiveCommands<String, String> redisReactiveClient,
            WorkerConfig workerConfig) {
        this.id = id;
        this.gson = gson;
        this.redisReactiveClient = redisReactiveClient;
        this.workerConfig = workerConfig;
    }

    private Entry parseJson(String message) {
        return gson.fromJson(message, Entry.class);
    }

    public abstract Entry handleMessage(Entry message);

    public void start() {
        redisReactiveClient.xreadgroup(
                Consumer.from(workerConfig.consumerGroupName(), getConsumerName()),
                XReadArgs.Builder.noack(),
                XReadArgs.StreamOffset.lastConsumed(workerConfig.messagesBacklogStreamKey())
            )
//            .flatMap(message ->
//                redisReactiveClient.xack(
//                    workerConfig.messagesBacklogStreamKey(), workerConfig.consumerGroupName(), message.getId()
//                )
//                .map($ -> message))
            .doOnNext(message -> System.out.println("Parsing " + message.getBody()))
//            .map(StreamMessage::getBody)
//            .filter(body -> !body.isEmpty() && body.get("message") != null)
//            .map(body -> {
//                Entry entry = this.parseJson(body.get("message"));
//
//                return this.handleMessage(entry);
//            })
//            .flatMap(result -> redisReactiveClient
//                .xadd(workerConfig.processedMessagesStreamKey(), "processed", gson.toJson(result)))
            .doOnError(ex -> {
                System.out.printf("Worker %d failed. Exiting...", this.id);

                this.deregisterWorkerInRedis();

                ex.printStackTrace();
            })
            .repeat()
            .subscribe();
    }

    protected void registerWorkerInRedis() {
        redisReactiveClient
            .rpush(workerConfig.activeConsumersListKey(), "" + this.id)
            .subscribe();
    }

    public void deregisterWorkerInRedis() {
        redisReactiveClient
            .lrem(this.workerConfig.activeConsumersListKey(), 1, "" + this.id)
            .subscribe();
    }

    private String getConsumerName() {
        return String.format("%s-%s", workerConfig.consumerGroupName(), this.id);
    }
}
