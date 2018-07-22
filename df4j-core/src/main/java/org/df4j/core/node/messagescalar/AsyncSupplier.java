package org.df4j.core.node.messagescalar;

import org.df4j.core.connector.messagescalar.ScalarPublisher;
import org.df4j.core.connector.messagescalar.ScalarSubscriber;
import org.df4j.core.node.AsyncProcedure;
import org.df4j.core.util.invoker.Invoker;
import org.df4j.core.util.invoker.RunnableInvoker;
import org.df4j.core.util.invoker.SupplierInvoker;

import java.util.function.Supplier;

/**
 * Base class for scalar nodes
 * Has predefined unbound output connector to keep the result of computation.
 *
 * Even if the computation does not produce a resulting value,
 * that connector is useful to monitor the end of the computation.
 *
 * @param <R>
 */
public class AsyncSupplier<R> extends AsyncProcedure<R> implements ScalarPublisher<R> {
    /** place for demands */
    protected final CompletablePromise<R> result = new CompletablePromise<>();

    public AsyncSupplier() {}

    public AsyncSupplier(Invoker<R> invoker) {
        super(invoker);
    }

    public AsyncSupplier(Supplier<R> proc) {
        super(new SupplierInvoker<>(proc));
    }

    public AsyncSupplier(Runnable proc) {
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
    protected R runAction() throws Exception {
        R value = super.runAction();
        result.complete(value);
        return value;
    }

    public String toString() {
        return super.toString() + result.toString();
    }

}
