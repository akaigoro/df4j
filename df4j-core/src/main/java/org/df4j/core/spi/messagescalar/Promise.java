package org.df4j.core.spi.messagescalar;

import org.df4j.core.impl.messagescalar.PortFuture;

import java.util.concurrent.Future;

/**
 * Promise is an asynchronous counterpart of {@link Future}.
 * While  {@link Future} deliver information to a synchronous activity (@link Thread),
 * {@link Promise) deliver information to an asynchronous activity (@link Port}.
 *
 * @param <T>
 */
public interface Promise<T> {

    void postTo(Port<T> subscriber);

    default Future<T> asFuture() {
        PortFuture<T> f = new PortFuture<T>();
        postTo(f);
        return f;
    }

}
