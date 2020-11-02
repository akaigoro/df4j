package org.df4j.protocol;

import java.util.concurrent.TimeUnit;

/**
 * A one-shot signal with completion exceptions
 */
public class Completable {
    private Completable() {}

    /**
     * Synchronous interface.
     */
    public interface Source {

        /**
         * @return true if this {@link Completable} was completed normally or exceptionally;
         * false otherwise
         */
        boolean isCompleted();

        /**
         * @return completion Exception, if this {@link Completable} was completed exceptionally;
         * null otherwise
         */
        Throwable getCompletionException();

        /**
         * @return true if this {@link Completable} was completed normally or exceptionally;
         * false otherwise
         */
        default boolean isCompletedExceptionally() {
            return getCompletionException() != null;
        }

        /**
         * waits this {@link Completable} to complete indefinetely
         */
        void await() throws InterruptedException;

        /**
         * waits this {@link Completable} to complete until timeout
         * @param timeoutMillis timeout in milliseconds
         * @return true if completed;
         *         false if timout reached
         */
        default boolean await(long timeout) throws InterruptedException {
            return await(timeout, TimeUnit.MILLISECONDS);
        }

        /**
         * waits this {@link Completable} to complete until timeout
         * @param timeout timeout in units
         * @param unit time unit
         * @return true if completed;
         *         false if timout reached
         */
        boolean await(long timeout, TimeUnit unit) throws InterruptedException;
    }

    /**
     * Asynchronous interface
     */
    public interface Publisher {

        void subscribe(Subscriber subscriber);

        boolean isCompleted();

        /**
         * @return completion Exception, if this {@link Completable} was completed exceptionally;
         * null otherwise
         */
        Throwable getCompletionException();

        /**
         * @return true if this {@link Completable} was completed normally or exceptionally;
         * false otherwise
         */
        default boolean isCompletedExceptionally() {
            return getCompletionException() != null;
        }
    }

    /**
     * Asynchronous interface
     */
    public interface Subscriber {

        void onSubscribe(SimpleSubscription subscription);

        /**
         * Called once if the deferred computation 'throws' an exception.
         * @param e the exception, not null.
         */
        void onError(Throwable e);

        /**
         * Called once the deferred computation completes normally.
         */
        void onComplete();
    }

}
