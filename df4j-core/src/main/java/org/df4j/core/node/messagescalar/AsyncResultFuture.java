package org.df4j.core.node.messagescalar;

import org.df4j.core.connector.messagescalar.ScalarPublisher;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.util.concurrent.*;
import java.util.function.*;

import static org.df4j.core.util.SameThreadExecutor.sameThreadExecutor;

/**
 * an equivalent to {@link java.util.concurrent.CompletableFuture}
 *
 * @param <R>
 */
public class AsyncResultFuture<R> extends AsyncResult<R> implements Future<R> {
    protected Future<R> future = asFuture();
    private boolean cancelled;

    public AsyncResultFuture() {
    }

    public AsyncResultFuture(Runnable runnable) {
        super(runnable);
    }

    @Override
    public boolean isDone() {
        return result.isDone();
    }

    @Override
    public boolean complete(R res) {
        return super.complete(res);
    }

    @Override
    public boolean completeExceptionally(Throwable ex) {
        return super.completeExceptionally(ex);
    }

    @Override
    public R get() throws InterruptedException, ExecutionException {
        if (cancelled) {
            throw new CancellationException();
        }
        return future.get();
    }

    @Override
    public R get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return future.get(timeout, unit);
    }

    /* ------------- Control and status methods -------------- */

    /**
     * If not already completed, completes this AsyncResultFuture with
     * a {@link CancellationException}. Dependent CompletableFutures
     * that have not already completed will also complete
     * exceptionally, with a {@link CompletionException} caused by
     * this {@code CancellationException}.
     *
     * @param mayInterruptIfRunning this value has no effect in this
     * implementation because interrupts are not used to control
     * processing.
     *
     * @return {@code true} if this task is now cancelled
     */
    public boolean cancel(boolean mayInterruptIfRunning) {
        cancelled = true;
        return false;
    }

    /**
     * Returns {@code true} if this AsyncResultFuture was cancelled
     * before it completed normally.
     *
     * @return {@code true} if this AsyncResultFuture was cancelled
     * before it completed normally
     */
    public boolean isCancelled() {
        return  cancelled;
    }

}
