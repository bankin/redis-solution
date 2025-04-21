package com.solution.monitoring;

import com.solution.monitoring.config.MonitoringConfig;
import io.github.cdimascio.dotenv.Dotenv;
import io.lettuce.core.Range;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.reactive.RedisReactiveCommands;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.time.Instant;

public class MonitoringMain {
    private static final Dotenv env = Dotenv.load();

    public static void start() {
        MonitoringConfig config = readConfig();
        int delayInSeconds = config.logDelayInSeconds();

        RedisURI uri = RedisURI.Builder
                .redis(config.redisHost(), config.redisPort())
                .build();

        RedisClient client = RedisClient.create(uri);

        RedisReactiveCommands<String, String> baseReactive = client.connect().reactive();

        Flux.interval(Duration.ofSeconds(delayInSeconds))
            .flatMap($ -> {
                long to = Instant.now().toEpochMilli();
                long from = to - delayInSeconds * 1000L;
                Range<String> range = Range.create("" + from, "" + to);

                return baseReactive
                    .xrange(config.processedMessageStreamKey(), range)
                    .count();
            })
            .doOnNext(count -> System.out.printf(
                "Parsed messages per second for last %d seconds %.2f%n", delayInSeconds, ((float) count) / delayInSeconds))
            .subscribe();
    }

    private static MonitoringConfig readConfig() {
        String host = env.get("REDIS_HOST", "localhost");
        int port = Integer.parseInt(env.get("REDIS_PORT", "6379"));
        String processedMessagesStreamKey = env.get("SOLUTION_PROCESSED_MESSAGES_STREAM_KEY", "messages:processed");
        int logDelayInSeconds = Integer.parseInt(env.get("SOLUTION_MONITORING_TIMEOUT_IN_SECONDS", "3"));

        return new MonitoringConfig(host, port, processedMessagesStreamKey, logDelayInSeconds);
    }

}