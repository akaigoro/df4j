package org.df4j.core.node.messagescalar;

import org.df4j.core.connector.messagescalar.CompletablePromise;
import org.df4j.core.connector.messagescalar.ScalarPublisher;
import org.df4j.core.connector.messagescalar.ScalarSubscriber;
import org.df4j.core.node.AsyncTask;

/**
 * Base class for scalar nodes
 * @param <R>
 */
public class AsyncResult<R> extends AsyncTask<R> implements ScalarPublisher<R> {
    /** place for demands */
    protected final CompletablePromise<R> result = new CompletablePromise<>();

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
            R res = runAction();
            complete(res); // wrong
        } catch (Throwable e) {
            stop();
            completeExceptionally(e);
        }
    }

    public String toString() {
        return super.toString() + result.toString();
    }

}
