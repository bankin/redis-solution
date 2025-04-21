package com.solution.transferer;

import com.solution.transferer.config.TransfererConfig;
import io.github.cdimascio.dotenv.Dotenv;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.reactive.RedisReactiveCommands;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import io.lettuce.core.pubsub.api.reactive.RedisPubSubReactiveCommands;

public class TransfererMain {
    private static final Dotenv env = Dotenv.load();

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
        String host = env.get("REDIS_HOST", "localhost");
        int port = Integer.parseInt(env.get("REDIS_PORT", "6379"));
        String pubSubKey = env.get("SOLUTION_PUB_SUB_KEY", "messages:published");
        String messageBacklogStreamKey = env.get("BANKIN_MESSAGE_BACKLOG_STREAM_KEY", "messages:backlog");

        return new TransfererConfig(host, port, pubSubKey, messageBacklogStreamKey);
    }
}