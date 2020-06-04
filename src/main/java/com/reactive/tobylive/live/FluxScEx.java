package com.reactive.tobylive.live;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Slf4j
public class FluxScEx {
    public static void main(String[] args) throws InterruptedException {
        Flux.interval(Duration.ofMillis(500))
                .take(10)
                .subscribe(s -> log.debug("onNext: {}", s));

        TimeUnit.SECONDS.sleep(10);
    }
}
