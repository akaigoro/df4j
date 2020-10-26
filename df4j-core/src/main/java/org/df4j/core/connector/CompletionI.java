package org.df4j.core.connector;

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
    LinkedList<CompletionSubscription> getSubscriptions();


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
}
