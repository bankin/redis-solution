package com.solution;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.solution.config.ConsumerConfig;
import com.solution.config.RedisConfig;
import com.solution.config.WorkerConfig;
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

    public static void start() {
        ConsumerConfig config = readConfig();
        WorkerConfig workerConfig = config.workerConfig();

        RedisURI uri = RedisURI.Builder
            .redis(config.redisConfig().host(), config.redisConfig().port())
            .build();
        RedisClient client = RedisClient.create(uri);

        RedisReactiveCommands<String, String> baseReactive = client.connect().reactive();

        createConsumerGroup(baseReactive, workerConfig.messagesBacklogStreamKey(), workerConfig.consumerGroupName())
            .flatMapMany($ -> Flux.range(1, config.consumerCount()))
            .flatMap(id ->
                baseReactive
                    .xgroupCreateconsumer(
                        workerConfig.messagesBacklogStreamKey(),
                        io.lettuce.core.Consumer.from(workerConfig.consumerGroupName(), "main-consumers-" + id)
                    )
                    .doOnNext($ -> new Consumer(gson, baseReactive, workerConfig).start()))
            .subscribe();
    }

    private static Mono<String> createConsumerGroup(
            RedisReactiveCommands<String, String> baseReactive,
            String streamKey,
            String consumerGroupName) {
        return baseReactive.xgroupDestroy(streamKey, consumerGroupName)
            .flatMap($ ->
                baseReactive.xgroupCreate(XReadArgs.StreamOffset.latest(streamKey), consumerGroupName, mkstream())
            );
    }

    private static ConsumerConfig readConfig() {
        String host = envOrDefault("REDIS_HOST", "localhost");
        int port = Integer.parseInt(envOrDefault("REDIS_PORT", "6379"));

        int consumerCount = Integer.parseInt(envOrDefault("SOLUTION_CONSUMER_COUNT", "1"));
        String activeConsumersListKey = envOrDefault("SOLUTION_ACTIVE_CONSUMERS_LIST_KEY", "consumer:ids");
        String processedMessagesStreamKey = envOrDefault("SOLUTION_PROCESSED_MESSAGES_STREAM_KEY", "messages:processed");

        String messageBacklogStreamKey = envOrDefault("BANKIN_MESSAGE_BACKLOG_STREAM_KEY", "messages:backlog");
        String consumerGroupName = envOrDefault("BANKIN_CONSUMER_GROUP_NAME", "main-consumers");

        return new ConsumerConfig(
            new RedisConfig(host, port),
            consumerCount,
            new WorkerConfig(messageBacklogStreamKey, activeConsumersListKey, consumerGroupName, processedMessagesStreamKey)
        );
    }

    private static String envOrDefault(String key, String defaultIfMissing) {
        String result = System.getenv(key);

        return result != null ? result : defaultIfMissing;
    }
}