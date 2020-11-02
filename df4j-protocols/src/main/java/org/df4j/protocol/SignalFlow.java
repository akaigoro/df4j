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

        /** Increases the number of permits by 1.
         * Analogue of {@link Flow.Subscriber#onNext(Object)} ()} and {@link java.util.concurrent.Semaphore#release()}.
         */
        default void release() {
            release(1);
        }

        /** Increases the number of permits.
         * Analogue of {@link Flow.Subscriber#onNext(Object)} ()} and {@link java.util.concurrent.Semaphore#release(int)}.
         *
         * @param n the number of additional permits.
         */
        void release(long n);
    }

}
