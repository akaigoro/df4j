package org.df4j.core.async;

import org.df4j.core.AsynchronousCall;
import org.df4j.core.Port;

public abstract class PromiseFunc<R> extends AsynchronousCall implements Promise<R> {
    CompletablePromise<R> out = new CompletablePromise<>();

    @Override
    public void postTo(Port<R> request) {
        out.postTo(request);
    }
}

