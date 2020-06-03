package com.reactive.tobylive.live;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

@Slf4j
public class ReactorEx {

    public static void main(String[] args) {
        Flux.<Integer>create(e -> {
            e.next(1);
            e.next(2);
            e.next(3);
            e.next(4);
            e.complete();
        })
                .log()
                .map(o -> o*10)
                .reduce(Integer::sum)
                .log()
                .subscribe(s -> log.debug(s.toString()));
    }

}
