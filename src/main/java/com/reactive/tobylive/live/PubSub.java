package com.reactive.tobylive.live;

import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;


//Pub -> Data1 -> Op1 -> Data2 -> Op2 -> Data3 -> Sub

@Slf4j
public class PubSub {

    public static void main(String[] args) {
        Publisher<Integer> pub = iterPub(Stream.iterate(1, integer -> integer + 1).limit(10).collect(Collectors.toUnmodifiableList()));
//        Publisher<Integer> mapPub = mapPub(pub, i -> i * 10);
//        Publisher<Integer> sumPub = sumPub(pub);
        Publisher<Integer> reducePub = reducePub(pub, 0, (a, b) -> a + b);

        reducePub.subscribe(logSub());
    }

    private static Publisher<Integer> reducePub(Publisher<Integer> pub, int seed, BiFunction<Integer, Integer, Integer> f) {
        return new Publisher<Integer>() {
            @Override
            public void subscribe(Subscriber<? super Integer> sub) {
                pub.subscribe(new DelegateSub<Integer>(sub){
                    int result = seed;
                    @Override
                    public void onNext(Integer integer) {
                        result = f.apply(result, integer);
                    }

                    @Override
                    public void onComplete() {
                       sub.onNext(result);
                       sub.onComplete();
                    }
                });
            }
        };
    }

    private static Publisher<Integer> sumPub(Publisher<Integer> pub) {
        return new Publisher<Integer>() {
            @Override
            public void subscribe(Subscriber<? super Integer> sub) {
                pub.subscribe(new DelegateSub<Integer>(sub) {
                    int sum = 0;

                    @Override
                    public void onNext(Integer integer) {
                        sum += integer;
                    }

                    @Override
                    public void onComplete() {
                        sub.onNext(sum);
                        sub.onComplete();
                    }
                });
            }
        };
    }

    private static <T> Publisher<T> mapPub(Publisher<T> pub, Function<T, T> f) {
        return new Publisher<T>() {
            @Override
            public void subscribe(Subscriber<? super T> sub) {
                pub.subscribe(new DelegateSub<T>(sub) {
                    @Override
                    public void onNext(T integer) {
                        sub.onNext(f.apply(integer));
                    }
                });
            }
        };
    }

    private static <T> Subscriber<T> logSub() {
        return new Subscriber<T>() {
            @Override
            public void onSubscribe(Subscription subscription) {
                log.debug("onSubscribe");
                subscription.request(Long.MAX_VALUE);
            }

            @Override
            public void onNext(T integer) {
                log.debug("onNext : {}", integer);
            }

            @Override
            public void onError(Throwable t) {
                log.debug("onError");
            }

            @Override
            public void onComplete() {
                log.debug("onComplete");
            }
        };
    }

    private static Publisher<Integer> iterPub(List<Integer> iter) {
        return new Publisher<Integer>() {
            @Override
            public void subscribe(Subscriber<? super Integer> sub) {
                sub.onSubscribe(new Subscription() {
                    @Override
                    public void request(long n) {
                        try {
                            iter.forEach(sub::onNext);
                            sub.onComplete();
                        } catch (Throwable t) {
                            sub.onError(t);
                        }
                    }

                    @Override
                    public void cancel() {

                    }
                });
            }
        };
    }
}
