package com.reactive.tobylive.chapter4;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;

import java.io.IOException;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RestController
@Slf4j
public class MyController {

    Queue<DeferredResult<String>> results = new ConcurrentLinkedDeque<>();

    @GetMapping("/callable")
    public Callable<String> callable() throws InterruptedException {
        log.info("callable");
        return () -> {
            log.info("async");
            Thread.sleep(2000); // 시간이 오래걸리는 작업.
            return "hello";
        };
    }

    @GetMapping("/dr")
    public DeferredResult<String> deferredResult() throws InterruptedException {
        log.info("deferredResult");
        DeferredResult<String> dr = new DeferredResult<>();
        results.add(dr);
        return dr;
    }

    @GetMapping("/dr/count")
    public String drCount() {
        return String.valueOf(results.size());
    }

    @GetMapping("/dr/event")
    public String drEvent(String msg) {
        for (DeferredResult<String> dr : results) {
            dr.setResult("Hello " + msg);
            results.remove();
        }
        return "OK";
    }

    @GetMapping("/emitter")
    public ResponseBodyEmitter emitter() {
        final ResponseBodyEmitter responseBodyEmitter = new ResponseBodyEmitter();

        Executors.newSingleThreadExecutor().submit(() -> {
            for (int i = 0; i < 50; i++) {
                try {
                    responseBodyEmitter.send("<p>Stream " + i + "</p>");
                    Thread.sleep(100);
                } catch (IOException | InterruptedException e) {
                }
            }
        });
        return responseBodyEmitter;
    }
}
