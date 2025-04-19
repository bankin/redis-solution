package com.solution;

import com.solution.config.TransfererConfig;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.reactive.RedisReactiveCommands;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import io.lettuce.core.pubsub.api.reactive.RedisPubSubReactiveCommands;

public class TransfererMain {

    public static void start() {
        TransfererConfig config = readConfig();

        RedisURI uri = RedisURI.Builder
                .redis(config.redisHost(), config.redisPort())
                .build();
        RedisClient client = RedisClient.create(uri);

        RedisReactiveCommands<String, String> baseReactive = client.connect().reactive();

        StatefulRedisPubSubConnection<String, String> connection = client.connectPubSub();
        RedisPubSubReactiveCommands<String, String> pubSubReactive = connection.reactive();

        pubSubReactive.subscribe(config.pubSubKey()).subscribe();
        pubSubReactive.observeChannels()
            .flatMap(message ->
                baseReactive.xadd(config.backlogStreamKey(), "message", message.getMessage()))
            .subscribe();
    }

    private static TransfererConfig readConfig() {
        String host = envOrDefault("REDIS_HOST", "localhost");
        int port = Integer.parseInt(envOrDefault("REDIS_PORT", "6379"));
        String pubSubKey = envOrDefault("SOLUTION_PUB_SUB_KEY", "messages:published");
        String messageBacklogStreamKey = envOrDefault("BANKIN_MESSAGE_BACKLOG_STREAM_KEY", "messages:backlog");

        return new TransfererConfig(host, port, pubSubKey, messageBacklogStreamKey);
    }

    private static String envOrDefault(String key, String defaultIfMissing) {
        String result = System.getenv(key);

        return result != null ? result : defaultIfMissing;
    }
}