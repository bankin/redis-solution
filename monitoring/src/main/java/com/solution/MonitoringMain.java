package com.solution;

import io.lettuce.core.Range;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.reactive.RedisReactiveCommands;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;

public class MonitoringMain {
    public static void start() {

        RedisURI uri = RedisURI.Builder
                .redis("localhost", 6379)
                .build();
        RedisClient client = RedisClient.create(uri);

        RedisReactiveCommands<String, String> baseReactive = client.connect().reactive();

        Flux.interval(Duration.ofSeconds(3))
            .flatMap($ -> {
                long to = Instant.now().toEpochMilli();
                long from = to - 3 * 1000;
                Range<String> range = Range.create("" + from, "" + to);

                return baseReactive
                        .xrange("messages:processed", range)
                        .count();
            })
            .doOnNext(count -> System.out.printf("Parsed messages per second for last 3 seconds %.2f%n", count / 3.0))
            .subscribe();
    }
}