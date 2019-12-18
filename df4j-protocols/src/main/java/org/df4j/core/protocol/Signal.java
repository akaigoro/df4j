package org.df4j.core.protocol;

public class Signal {

    private Signal() {}

    /**
     * A {@link Publisher} is a provider of a potentially unbounded number of permits
     */
    public interface Publisher {

        /**
         * asynchronous version of Semaphore.aquire()
         *
         * @param subscriber
         *      the {@link Subscriber} that will consume signals from this {@link Signal.Publisher}
         */
        void subscribe(Subscriber subscriber);

        /**
         *
         * @param subscriber {@link Subscriber} which does not want to wait for data anymore
         * @return true if subscriber was subscribed and is unsubscribed now
         */
        boolean unsubscribe(Subscriber subscriber);
    }

    public interface Subscriber {
        /**
         * asynchronous version of Semaphore.aquire()
         */
        void onComplete();
    }
}
