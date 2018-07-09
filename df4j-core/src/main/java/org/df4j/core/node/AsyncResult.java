package org.df4j.core.node;

import org.df4j.core.connector.messagescalar.CompletablePromise;
import org.df4j.core.connector.messagescalar.ScalarPublisher;
import org.df4j.core.connector.messagescalar.ScalarSubscriber;

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
        return result.complete(res);
    }

    protected boolean completeExceptionally(Throwable ex) {
        return result.completeExceptionally(ex);
    }

    @Override
    public void run() {
        try {
            super.run();
            result.complete(null);
        } catch (Exception e) {
            result.completeExceptionally(e);
        }
    }

    public String toString() {
        return super.toString() + result.toString();
    }
}
