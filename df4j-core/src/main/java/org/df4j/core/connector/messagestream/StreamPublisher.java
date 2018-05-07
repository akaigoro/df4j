package org.df4j.core.connector.messagestream;

public interface StreamPublisher<M> {

    <S extends StreamSubscriber<? super M>> S subscribe(S subscriber);
}
