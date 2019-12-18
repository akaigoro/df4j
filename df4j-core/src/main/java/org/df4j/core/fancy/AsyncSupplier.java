package org.df4j.core.fancy;

import org.df4j.core.protocol.Scalar;
import org.df4j.core.fancy.invoker.Invoker;
import org.df4j.core.fancy.invoker.RunnableInvoker;
import org.df4j.core.fancy.invoker.SupplierInvoker;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

/**
 * Base class for scalar nodes
 * Has predefined unbound output connector to keep the result of computation.
 * Even if the computation does not produce a resulting value,
 * that connector is useful to monitor the end of the computation.
 *
 * @param <R> type of the result
 */
public class AsyncSupplier<R> extends AsyncAction<R> implements Scalar.Publisher<R>, Future<R> {

    public AsyncSupplier() {}

    public AsyncSupplier(Invoker invoker) {
        super(invoker);
    }

    public AsyncSupplier(Supplier<R> proc) {
        super(new SupplierInvoker<>(proc));
    }

    public AsyncSupplier(Runnable proc) {
        super(new RunnableInvoker<R>(proc));
    }

    @Override
    public void subscribe(Scalar.Subscriber<? super R> subscriber) {
        asyncResult().subscribe(subscriber);
    }

    protected void completeResult(R res) {
        asyncResult().onSuccess(res);
    }

    protected void completeResultExceptionally(Throwable ex) {
        result.onError(ex);
    }

    @Override
    public boolean cancel(boolean b) {
        return result.cancel(b);
    }

    @Override
    public boolean isCancelled() {
        return result.isCancelled();
    }

    @Override
    public boolean isDone() {
        return result.isDone();
    }

    @Override
    public R get() throws InterruptedException, ExecutionException {
        return asyncResult().get();
    }

    @Override
    public R get(long l, TimeUnit timeUnit) throws InterruptedException, ExecutionException, TimeoutException {
        return asyncResult().get(l, timeUnit);
    }

    @Override
    protected void run() {
        try {
            R res = callAction();
            result.onSuccess(res);
        } catch (Throwable e) {
            result.onError(e);
        }
    }
}
