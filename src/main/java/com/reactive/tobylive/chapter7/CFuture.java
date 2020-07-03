package com.reactive.tobylive.chapter7;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.*;

@Slf4j
public class CFuture {
    public static void main(String[] args) throws InterruptedException {
        ExecutorService es = Executors.newFixedThreadPool(10);
        CompletableFuture
                .supplyAsync(() -> {
                    log.info("supplyAsync");
                    return 1;
                }, es)
                .thenCompose(integer -> {
                    log.info("thenCompose : {}", integer);
                    return CompletableFuture.completedFuture(integer + 1);
                })
                .thenApplyAsync(integer -> {
                    log.info("thenApply : {}", integer);
                    return integer * 3;
                }, es)
                .exceptionally(e -> -10)
                .thenAcceptAsync(s -> log.info("thenAccept : {}", s), es);

        log.info("exit");

        es.shutdown();
        es.awaitTermination(10, TimeUnit.SECONDS);

        ForkJoinPool.commonPool().shutdown();
        ForkJoinPool.commonPool().awaitTermination(10, TimeUnit.SECONDS);
    }

}
