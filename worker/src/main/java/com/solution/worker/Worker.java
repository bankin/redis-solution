package com.solution.worker;

import com.google.gson.Gson;
import com.solution.config.WorkerConfig;
import com.solution.model.Entry;
import io.lettuce.core.api.reactive.RedisReactiveCommands;

import java.util.Random;

public class Worker extends BaseWorker {
    private final static Random rand = new Random();

    public Worker(
        int id,
        Gson gson,
        RedisReactiveCommands<String, String> redisListClient,
        WorkerConfig workerConfig
    ) {
        super(id, gson, redisListClient, workerConfig);

        registerWorkerInRedis();
    }

    @Override
    public Entry handleMessage(Entry entry) {
        entry.setHandledData(rand.nextInt());

        return entry;
    }
}
