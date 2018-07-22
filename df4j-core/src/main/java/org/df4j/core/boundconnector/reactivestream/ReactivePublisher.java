package org.df4j.core.boundconnector.reactivestream;

public interface ReactivePublisher<M> {

    <S extends ReactiveSubscriber<? super M>> S subscribe(S ReactiveSubscriber);

}
