package com.reactive.tobylive.chapter6;

import io.netty.channel.nio.NioEventLoopGroup;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.Netty4ClientHttpRequestFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.AsyncRestTemplate;
import org.springframework.web.context.request.async.DeferredResult;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * 스프링 리액티브 웹 개발 6부
 */
@SpringBootApplication
@EnableAsync
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
            DeferredResult<String> deferredResult = new DeferredResult<>();
            Completion.from(rt.getForEntity(URL1, String.class, "hello" + idx))
                    .andApply(s -> rt.getForEntity(URL2, String.class, s.getBody()))
                    .andApply(s -> myService.work(s.getBody()))
                    .andError(e -> deferredResult.setErrorResult(e.toString()))
                    .andAccept(s -> deferredResult.setResult(s));

//            final ListenableFuture<ResponseEntity<String>> f1 = rt.getForEntity(URL1, String.class, "hello" + idx);
//            f1.addCallback(result -> {
//                final ListenableFuture<ResponseEntity<String>> f2 = rt.getForEntity(URL2, String.class, result.getBody());
//                f2.addCallback(s2 -> {
//                    final ListenableFuture<String> f3 = myService.work(s2.getBody());
//                    f3.addCallback(s3 -> {
//                        deferredResult.setResult(s3);
//                    }, ex -> {
//                        deferredResult.setErrorResult(ex.getMessage());
//                    });
//                }, ex -> {
//                    deferredResult.setErrorResult(ex.getMessage());
//                });
//            }, ex -> {
//                deferredResult.setErrorResult(ex.getMessage());
//            });

            return deferredResult;
        }
    }

    public static class AcceptCompletion<S> extends Completion<S, Void> {
        Consumer<S> consumer;

        public AcceptCompletion(Consumer<S> consumer) {
            this.consumer = consumer;
        }


        @Override
        void run(S value) {
            consumer.accept(value);
        }
    }

    public static class ErrorCompletion<T> extends Completion<T, T> {
        Consumer<Throwable> econ;

        public ErrorCompletion(Consumer<Throwable> econ) {
            this.econ = econ;
        }

        @Override
        void run(T value) {
            if (next != null) {
                next.run(value);
            }
        }

        @Override
        void error(Throwable e) {
            econ.accept(e);
        }
    }

    public static class ApplyCompletion<S, T> extends Completion<S, T> {
        Function<S, ListenableFuture<T>> fn;

        public ApplyCompletion(Function<S, ListenableFuture<T>> fn) {
            this.fn = fn;
        }

        @Override
        void run(S value) {
            final ListenableFuture<T> lf = fn.apply(value);
            lf.addCallback(this::complete, this::error);
        }
    }

    public static class Completion<S, T> {
        Completion next;

        public static <S, T> Completion<S, T> from(ListenableFuture<T> listenableFuture) {
            Completion<S, T> c = new Completion<>();
            listenableFuture.addCallback(c::complete, c::error);
            return c;
        }

        public <V> Completion<T, V> andApply(Function<T, ListenableFuture<V>> fn) {
            Completion<T, V> c = new ApplyCompletion<>(fn);
            this.next = c;
            return c;
        }

        public Completion<T, T> andError(Consumer<Throwable> econ) {
            Completion<T, T> c = new ErrorCompletion<>(econ);
            next = c;
            return c;
        }

        public void andAccept(Consumer<T> consumer) {
            Completion<T, Void> c = new AcceptCompletion<>(consumer);
            next = c;
        }


        void complete(T s) {
            if (next != null) {
                next.run(s);
            }
        }

        void run(S value) {
        }

        void error(Throwable e) {
            if (next != null) {
                next.error(e);
            }
        }

    }

    @Service
    public static class MyService {
        @Async
        public ListenableFuture<String> work(String req) {
            return new AsyncResult<>(req + "/asyncwork");
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

}