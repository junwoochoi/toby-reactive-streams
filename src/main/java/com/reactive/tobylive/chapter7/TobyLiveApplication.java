package com.reactive.tobylive.chapter7;

import io.netty.channel.nio.NioEventLoopGroup;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.Netty4ClientHttpRequestFactory;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.AsyncRestTemplate;
import org.springframework.web.context.request.async.DeferredResult;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * 스프링 리액티브 웹 개발 7부 -> CompletableFuture 에 대해
 */
@SpringBootApplication
@EnableAsync
@Slf4j
public class TobyLiveApplication {
    @RestController
    @RequiredArgsConstructor
    public static class MyController {

        public static final String URL1 = "http://localhost:8081/service1?req={req}";
        public static final String URL2 = "http://localhost:8081/service2?req={req}";
        private final MyService myService;
        private final AsyncRestTemplate rt = new AsyncRestTemplate(new Netty4ClientHttpRequestFactory(new NioEventLoopGroup(1)));

        @GetMapping("/rest")
        public DeferredResult<String> rest(int idx) {
            DeferredResult<String> dr = new DeferredResult<>();

            toCompletableFuture(rt.getForEntity(URL1, String.class, "h" + idx))
                    .thenCompose(s -> {
                        log.info("thenCompose");
                        return toCompletableFuture(rt.getForEntity(URL2, String.class, s.getBody()));
                    })
                    .thenApplyAsync(s -> {
                        log.info("thenApplyAsync");
                        return myService.work(s.getBody());
                    }, Executors.newCachedThreadPool())
                    .thenAccept(s -> {
                        log.info("thenAccept");
                        dr.setResult(s);
                    })
                    .exceptionally(throwable -> {
                        dr.setErrorResult(throwable.getMessage());
                        return null;
                    });
            return dr;
        }
    }


    @Service
    public static class MyService {
        public String work(String req) {
            return req + "/asyncwork";
        }

    }

    @Bean
    ThreadPoolTaskExecutor myThreadPool() {
        final ThreadPoolTaskExecutor threadPoolTaskExecutor = new ThreadPoolTaskExecutor();
        threadPoolTaskExecutor.setCorePoolSize(1);
        threadPoolTaskExecutor.setMaxPoolSize(1);
        threadPoolTaskExecutor.initialize();
        return threadPoolTaskExecutor;
    }

    public static void main(String[] args) {
        SpringApplication.run(TobyLiveApplication.class, args);
    }


    static <T> CompletableFuture<T> toCompletableFuture(ListenableFuture<T> listenableFuture) {
        final CompletableFuture<T> completableFuture = new CompletableFuture<>();
        listenableFuture.addCallback(completableFuture::complete, completableFuture::completeExceptionally);
        return completableFuture;
    }
}
