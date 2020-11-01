package org.df4j.reactor.actor;

import org.junit.Test;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import reactor.core.publisher.Flux;

public class FluxTest {
    @Test
    public void test1() {
        Flux flux = Flux.range(0,10);
        Subscriber subs = new Subscriber() {
            @Override
            public void onSubscribe(Subscription s) {

            }

            @Override
            public void onNext(Object o) {

            }

            @Override
            public void onError(Throwable t) {

            }

            @Override
            public void onComplete() {

            }
        };
        flux.subscribe(subs);
        flux.subscribe(subs);
    }
}
