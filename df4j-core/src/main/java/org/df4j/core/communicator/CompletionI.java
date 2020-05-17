package org.df4j.core.communicator;

import org.df4j.protocol.Completable;
import org.df4j.protocol.SimpleSubscription;

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

    void complete();

    void completeExceptionally(Throwable e);

        /**
         * @return true if this {@link Completable} was completed normally or exceptionally;
         * false otherwise
         */
    boolean isCompleted();

    void join()  throws InterruptedException;

    boolean blockingAwait(long timeoutMillis);

    boolean blockingAwait(long timeout, TimeUnit unit);
}
