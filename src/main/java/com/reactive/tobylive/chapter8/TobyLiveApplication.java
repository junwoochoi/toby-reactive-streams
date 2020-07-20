package com.reactive.tobylive.chapter8;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.config.EnableWebFlux;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.concurrent.CompletableFuture;

/**
 * 스프링 리액티브 웹 개발 8부 -> Reactive Programming - WebFlux 에 대해
 */
@SpringBootApplication
@EnableWebFlux
@Slf4j
@EnableAsync
public class TobyLiveApplication {

    @Autowired
    private MyService myService;
    WebClient client = WebClient.create();

    @RestController
    @RequiredArgsConstructor
    public class MyController {
        public static final String URL1 = "http://localhost:8081/service1?req={req}";
        public static final String URL2 = "http://localhost:8081/service2?req={req}";

        @GetMapping("/rest")
        public Mono<String> rest(int idx) {
            return client.get().uri(URL1, idx).exchange()
                    .log()
                    .flatMap(res -> res.bodyToMono(String.class))
                    .flatMap(s -> client.get().uri(URL2, s).exchange())
                    .log()
                    .flatMap(res -> res.bodyToMono(String.class))
                    .flatMap(s -> Mono.fromCompletionStage(myService.work(s)));
        }
    }

    public static void main(String[] args) {
        SpringApplication.run(TobyLiveApplication.class, args);
    }

    @Service
    public static class MyService {
        @Async
        public CompletableFuture<String> work(String req) {
            return CompletableFuture.completedFuture(req + "/asyncwork");
        }
    }

}
