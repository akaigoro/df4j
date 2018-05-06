package org.df4j.core.core.connector.reactivestream;

public interface Publisher<M> {

    void subscribe(Subscriber<? super M> subscriber);
}
