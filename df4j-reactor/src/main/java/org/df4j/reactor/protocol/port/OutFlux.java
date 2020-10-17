package org.df4j.reactor.protocol.port;

import org.df4j.core.port.OutMessagePort;
import org.df4j.protocol.Flow;
import reactor.core.CoreSubscriber;
import reactor.core.publisher.Flux;

public class OutFlux<T> extends Flux<T> implements Flow.Publisher<T>, OutMessagePort<T> {
    @Override
    public void subscribe(CoreSubscriber<? super T> actual) {
        
    }

    @Override
    public void onNext(T message) {
    }

    @Override
    public void onComplete() {

    }

    @Override
    public void onError(Throwable ex) {

    }
}
