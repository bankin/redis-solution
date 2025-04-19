package com.solution.monitoring;

import com.solution.monitoring.config.MonitoringConfig;
import io.lettuce.core.Range;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.reactive.RedisReactiveCommands;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.time.Instant;

public class MonitoringMain {
    public static void start() {
        MonitoringConfig config = readConfig();

        RedisURI uri = RedisURI.Builder
                .redis(config.redisHost(), config.redisPort())
                .build();

        RedisClient client = RedisClient.create(uri);

        RedisReactiveCommands<String, String> baseReactive = client.connect().reactive();

        Flux.interval(Duration.ofSeconds(3))
            .flatMap($ -> {
                long to = Instant.now().toEpochMilli();
                long from = to - 3 * 1000;
                Range<String> range = Range.create("" + from, "" + to);

                return baseReactive
                        .xrange(config.processedMessageStreamKey(), range)
                        .count();
            })
            .doOnNext(count -> System.out.printf("Parsed messages per second for last 3 seconds %.2f%n", count / 3.0))
            .subscribe();
    }

    private static MonitoringConfig readConfig() {
        String host = envOrDefault("REDIS_HOST", "localhost");
        int port = Integer.parseInt(envOrDefault("REDIS_PORT", "6379"));
        String processedMessagesStreamKey = envOrDefault("SOLUTION_PROCESSED_MESSAGES_STREAM_KEY", "messages:processed");

        return new MonitoringConfig(host, port, processedMessagesStreamKey);
    }

    private static String envOrDefault(String key, String defaultIfMissing) {
        String result = System.getenv(key);

        return result != null ? result : defaultIfMissing;
    }


}