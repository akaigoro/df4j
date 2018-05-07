package org.df4j.core.connector.reactivestream;

public interface Publisher<M> {

    void subscribe(Subscriber<? super M> subscriber);
}
