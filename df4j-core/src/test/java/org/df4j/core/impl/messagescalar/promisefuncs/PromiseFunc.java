package org.df4j.core.impl.messagescalar.promisefuncs;

import org.df4j.core.impl.AsynchronousCall;
import org.df4j.core.impl.messagescalar.PortPromise;
import org.df4j.core.spi.messagescalar.Port;
import org.df4j.core.spi.messagescalar.Promise;

public abstract class PromiseFunc<R> extends AsynchronousCall implements Promise<R> {
    protected PortPromise<R> out = new PortPromise<>();

    @Override
    public void postTo(Port<R> request) {
        out.postTo(request);
    }
}

