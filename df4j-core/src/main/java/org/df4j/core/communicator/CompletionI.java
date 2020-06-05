package org.df4j.core.communicator;

import org.df4j.protocol.Completable;

import java.util.LinkedList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;

/**
 * Completes successfully or with failure, without emitting any value.
 * Similar to {@link CompletableFuture}&lt;Void&gt;
 */
public interface CompletionI extends Completable.Source {
    LinkedList<Completion.CompletionSubscription> getSubscriptions();

    /**
     * @return completion Exception, if this {@link Completable} was completed exceptionally;
     * null otherwise
     */
    Throwable getCompletionException();

    /**
     * sets the state to completed
     */
    void complete();

    /**
     * If not already completed, causes invocations of get() and related methods to throw {@link CompletionException}
     * with the given exception as the cause.
     * @param e exception to throw
     */
    void completeExceptionally(Throwable e);

    /**
     * @return true if this {@link Completable} was completed normally or exceptionally;
     * false otherwise
     */
    boolean isCompleted();

    /**
     * waits this {@link Completable} to complete indefinetely
     */
    void join()  throws InterruptedException;

    /**
     * waits this {@link Completable} to complete until timeout
     * @param timeoutMillis timeout in milliseconds
     * @return true if completed;
     *         false if timout reached
     */
    boolean blockingAwait(long timeoutMillis);

    /**
     * waits this {@link Completable} to complete until timeout
     * @param timeout timeout in units
     * @param unit time unit
     * @return true if completed;
     *         false if timout reached
     */
    boolean blockingAwait(long timeout, TimeUnit unit);
}
