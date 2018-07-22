package org.df4j.core.boundconnector.messagestream;

public interface StreamPublisher<M> {

    <S extends StreamSubscriber<? super M>> S subscribe(S subscriber);
}
