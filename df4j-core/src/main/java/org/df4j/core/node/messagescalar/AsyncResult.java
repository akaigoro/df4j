package org.df4j.core.node.messagescalar;

import org.df4j.core.connector.messagescalar.ScalarPublisher;
import org.df4j.core.connector.messagescalar.ScalarSubscriber;
import org.df4j.core.node.AsyncTask;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.function.*;

import static org.df4j.core.util.SameThreadExecutor.sameThreadExecutor;

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

    /**
     * Returns {@code true} if this AsyncResult completed
     * exceptionally, in any way. Possible causes include
     * cancellation, explicit invocation of {@code
     * completeExceptionally}, and abrupt termination of a
     * AsyncResult action.
     *
     * @return {@code true} if this AsyncResult completed
     * exceptionally
     */
    public boolean isCompletedExceptionally() {
        return result.isCompletedExceptionally();
    }

    /**
     * Forcibly sets or resets the value subsequently returned by
     * method {@link #get()} and related methods, whether or not
     * already completed. This method is designed for use only in
     * error recovery actions, and even in such situations may result
     * in ongoing dependent completions using established versus
     * overwritten outcomes.
     *
     * @param value the completion value
     */
    public void obtrudeValue(R value) {
        throw new NotImplementedException();
    }

    /**
     * Forcibly causes subsequent invocations of method {@link #get()}
     * and related methods to throw the given exception, whether or
     * not already completed. This method is designed for use only in
     * error recovery actions, and even in such situations may result
     * in ongoing dependent completions using established versus
     * overwritten outcomes.
     *
     * @param ex the exception
     * @throws NullPointerException if the exception is null
     */
    public void obtrudeException(Throwable ex) {
        throw new NotImplementedException();
    }

    /**
     * Returns the estimated number of CompletableFutures whose
     * completions are awaiting completion of this AsyncResult.
     * This method is designed for use in monitoring system state, not
     * for synchronization control.
     *
     * @return the number of dependent CompletableFutures
     */
    public int getNumberOfDependents() {
        return result.getNumberOfSubscribers();
    }

    /**
     * Returns a string identifying this AsyncResult, as well as
     * its completion state.  The state, in brackets, contains the
     * String {@code "Completed Normally"} or the String {@code
     * "Completed Exceptionally"}, or the String {@code "Not
     * completed"} followed by the number of CompletableFutures
     * dependent upon its completion, if any.
     *
     * @return a string identifying this AsyncResult, as well as its state
     */
    public String toString() {
        return super.toString() + result.toString();
    }
}
