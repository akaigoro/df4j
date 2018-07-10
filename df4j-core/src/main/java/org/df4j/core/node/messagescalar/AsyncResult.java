package org.df4j.core.node.messagescalar;

import org.df4j.core.connector.messagescalar.CompletablePromise;
import org.df4j.core.connector.messagescalar.ScalarPublisher;
import org.df4j.core.connector.messagescalar.ScalarSubscriber;
import org.df4j.core.node.AsyncTask;
import org.df4j.core.util.invoker.Invoker;
import org.df4j.core.util.invoker.RunnableInvoker;
import org.df4j.core.util.invoker.SupplierInvoker;

import java.util.function.Supplier;

/**
 * Base class for scalar nodes
 * @param <R>
 */
public class AsyncResult<R> extends AsyncTask<R> implements ScalarPublisher<R> {
    /** place for demands */
    protected final CompletablePromise<R> result = new CompletablePromise<>();

    public AsyncResult() {}

    public AsyncResult(Invoker<R> invoker) {
        super(invoker);
    }

    public AsyncResult(Supplier<R> proc) {
        super(new SupplierInvoker<>(proc));
    }

    public AsyncResult(Runnable proc) {
        super(new RunnableInvoker<R>(proc));
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
