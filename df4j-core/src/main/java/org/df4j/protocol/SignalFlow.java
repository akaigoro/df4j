package org.df4j.protocol;

import org.reactivestreams.Subscription;

/**
 * Flow of signals without errors
 * It operates excactly as {@link Flow} but without message information.
 */
public class SignalFlow {

    private SignalFlow() {}

    /**
     * A {@link Publisher} is a provider of a potentially unbounded number of permits
     */
    public interface Publisher {

        /**
         * asynchronous version of Semaphore.aquire()
         *
         * @param subscriber
         *      the {@link Subscriber} that will consume signals from this {@link SignalFlow.Publisher}
         */
        void subscribe(Subscriber subscriber);
    }

    /**
     *  inlet for permits.
     *
     */
    public interface Subscriber {

        void onSubscribe(Subscription subscription);

        /** Decreases the number of permits.
         * Analogue of {@link Flow.Subscriber#onNext(Object)} ()} and {@link java.util.concurrent.Semaphore#acquire(int)}.
         * If nomber of permits become non-positive, the call int thread is not blocked or gets an exception.
         * The effect is deferred: correspondig port is blocked and the enclosing actor won't execute.
         *
         * @param n the number of additional permits.
         */
        void acquire(long n);

        /** Decreases the number of permits by 1.
         * Analogue of {@link java.util.concurrent.Semaphore#acquire}.
         */
        default void acquire() {
            acquire(1);
        }

        /** Increases the number of permits.
         * Analogue of {@link Flow.Subscriber#onNext(Object)} ()} and {@link java.util.concurrent.Semaphore#release(int)}.
         * If total number of permits become positive, correspondig port is unblocked.
         *
         * @param n the number of additional permits.
         */
        void release(long n);

        /** Increases the number of permits by 1.
         * Analogue of {@link Flow.Subscriber#onNext(Object)} ()} and {@link java.util.concurrent.Semaphore#release()}.
         */
        default void release() {
            release(1);
        }
    }

}
