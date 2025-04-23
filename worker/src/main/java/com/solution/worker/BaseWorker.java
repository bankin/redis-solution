package com.solution.worker;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.solution.config.WorkerConfig;
import com.solution.model.Entry;
import io.lettuce.core.Consumer;
import io.lettuce.core.StreamMessage;
import io.lettuce.core.XReadArgs;
import io.lettuce.core.api.reactive.RedisReactiveCommands;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Optional;

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

    /**
     * Base method to process a messages coming though Redis
     *
     * @param message the received message
     *
     * @return parsed message. The result will be written back to Redis
     */
    public abstract Entry handleMessage(Entry message);

    /**
     * Start the flow of a worker - Reading -> Processing -> Writing
     * Reading through a redis stream with consumer group applied.
     *
     * Entries without a valid message are skipped
     *
     * If a worker throws an error it is stopped and deregistered from the Redis list of active workers
     *
     * @return a stream of processed messages
     */
    public Flux<Entry> start() {
        return redisReactiveClient.xreadgroup(
                Consumer.from(workerConfig.consumerGroupName(), getConsumerName()),
                XReadArgs.Builder.noack(),
                XReadArgs.StreamOffset.lastConsumed(workerConfig.messagesBacklogStreamKey())
            )
            .map(StreamMessage::getBody)
            .filter(body -> !body.isEmpty() && body.get("message") != null)
            .map(this::processAndReport)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .doOnError(ex -> {
                System.out.printf("Worker %d failed. Exiting...", this.id);

                this.deregisterWorkerInRedis();

                ex.printStackTrace();
            })
            .repeat();
    }

    private Optional<Entry> processAndReport(Map<String, String> body) {
        return this.parseJson(body.get("message"))
            .map(this::handleMessage)
            .map(result -> {
                redisReactiveClient
                    .xadd(workerConfig.processedMessagesStreamKey(), "processed", gson.toJson(result))
                    .subscribe();

                return result;
            });
    }

    protected Mono<Void> registerWorkerInRedis() {
        return redisReactiveClient
            .rpush(workerConfig.activeConsumersListKey(), "" + this.id)
            .then();
    }

    public Mono<Void> deregisterWorkerInRedis() {
        return redisReactiveClient
            .lrem(this.workerConfig.activeConsumersListKey(), 1, "" + this.id)
            .then();
    }

    private String getConsumerName() {
        return String.format("%s-%s", workerConfig.consumerGroupName(), this.id);
    }

    private Optional<Entry> parseJson(String message) {
        try {
            return Optional.of(gson.fromJson(message, Entry.class));
        } catch (JsonSyntaxException ex) {
            System.out.println("Message is not a valid Entry " + ex.getMessage());

            return Optional.empty();
        }
    }
}
