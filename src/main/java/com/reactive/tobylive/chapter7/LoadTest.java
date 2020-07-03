package com.reactive.tobylive.chapter7;

import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StopWatch;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class LoadTest {
    static AtomicInteger counter = new AtomicInteger(0);

    public static void main(String[] args) throws InterruptedException, BrokenBarrierException {
        testRest();
    }

    private static void testRest() throws InterruptedException, BrokenBarrierException {
        ExecutorService es = Executors.newFixedThreadPool(100);
        RestTemplate rt = new RestTemplate();
        final String url = "http://localhost:8080/rest?idx={idx}";

        final CyclicBarrier barrier = new CyclicBarrier(101);


        for (int i = 0; i < 100; i++) {
            es.submit(() -> {
                int idx = counter.addAndGet(1);
                log.info("Thread {} ", idx);

                barrier.await();

                StopWatch stopWatch = new StopWatch();
                stopWatch.start();

                final String res = rt.getForObject(url, String.class, idx);

                stopWatch.stop();
                log.info("Elapsed: {} -> {} / {}", idx, stopWatch.getTotalTimeSeconds(), res);
                return null;
            });
        }

        barrier.await();
        StopWatch main = new StopWatch();
        main.start();

        es.shutdown();
        es.awaitTermination(600, TimeUnit.SECONDS);

        main.stop();
        log.info("total : {}", main.getTotalTimeSeconds());
    }

    private static void testCallable() throws InterruptedException, BrokenBarrierException {
        ExecutorService es = Executors.newFixedThreadPool(100);
        RestTemplate rt = new RestTemplate();
        final String url = "http://localhost:8080/callable";

        CyclicBarrier barrier = new CyclicBarrier(100);

        for (int i = 0; i < 100; i++) {
            es.submit(() -> {
                int idx = counter.addAndGet(1);
                barrier.await();

                log.info("Thread {} ", idx);

                StopWatch stopWatch = new StopWatch();
                stopWatch.start();

                rt.getForObject(url, String.class);

                stopWatch.stop();
                log.info("Elapsed: {} -> {}", idx, stopWatch.getTotalTimeSeconds());
                return null;
            });
        }

        barrier.await();
        StopWatch main = new StopWatch();
        main.start();

        es.shutdown();
        es.awaitTermination(100, TimeUnit.SECONDS);

        main.stop();
        log.info("total : {}", main.getTotalTimeSeconds());
    }
}
