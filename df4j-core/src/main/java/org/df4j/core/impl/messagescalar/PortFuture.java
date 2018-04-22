package org.df4j.core.impl.messagescalar;

import org.df4j.core.spi.messagescalar.Port;

import java.util.concurrent.*;

public class PortFuture<T> extends CompletableFuture<T> implements Port<T> {

    @Override
    public void post(T message) {
        super.complete(message);
    }

    @Override
    public void postFailure(Throwable ex) {
        super.completeExceptionally(ex);
    }

}
