package org.df4j.core.impl.messagescalar;

import org.df4j.core.impl.AsynchronousCall;
import org.df4j.core.spi.messagescalar.Port;
import org.df4j.core.spi.messagescalar.Promise;

public abstract class PromiseFunc<R> extends AsynchronousCall implements Promise<R> {
    PortPromise<R> out = new PortPromise<>();

    @Override
    public void postTo(Port<R> request) {
        out.postTo(request);
    }
}

