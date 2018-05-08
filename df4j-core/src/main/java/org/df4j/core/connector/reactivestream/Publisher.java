package org.df4j.core.connector.reactivestream;

public interface Publisher<M> {

    <S extends Subscriber<? super M>> S subscribe(S subscriber);

}
