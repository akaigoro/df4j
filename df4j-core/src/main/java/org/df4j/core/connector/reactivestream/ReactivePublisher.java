package org.df4j.core.connector.reactivestream;

public interface ReactivePublisher<M> {

    <S extends ReactiveSubscriber<? super M>> S subscribe(S ReactiveSubscriber);

}
