package org.df4j.core.node.messagescalar;

import org.df4j.core.connector.messagescalar.CompletablePromise;
import org.df4j.core.connector.messagescalar.ScalarPublisher;
import org.df4j.core.connector.messagescalar.ScalarSubscriber;
import org.df4j.core.node.AsyncTask;

/**
 * a node with an output parameter
 *
 * @param <R>
 */
public class AsyncResult<R> extends AsyncTask implements ScalarPublisher<R> {
    /** place for demands */
    protected final CompletablePromise<R> result = new CompletablePromise<>();

    public AsyncResult() {
    }

    public AsyncResult(Runnable runnable) {
        super(runnable);
    }

    public CompletablePromise<R> asyncResult() {
        return result;
    }


    @Override
    public <S extends ScalarSubscriber<? super R>> S subscribe(S subscriber) {
        result.subscribe(subscriber);
        return subscriber;
    }

    protected boolean complete(R res) {
        result.complete(res);
        return false; // TODO FIX
    }

    protected boolean completeExceptionally(Throwable ex) {
        result.completeExceptionally(ex);
        return false; // TODO FIX
    }

    public String toString() {
        return super.toString() + result.toString();
    }
}
