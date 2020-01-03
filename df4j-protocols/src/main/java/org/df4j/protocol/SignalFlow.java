package org.df4j.protocol;

import org.reactivestreams.Subscription;

/**
 * Flow of signals without errors
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

        /**
         * asynchronous version of Semaphore.aquire()
         */
        void awake();
    }

}
