package org.df4j.protocol;

public class SignalStream {

    private SignalStream() {}

    /**
     * A {@link Publisher} is a provider of a potentially unbounded number of permits
     */
    public interface Publisher {

        /**
         * asynchronous version of Semaphore.aquire()
         *
         * @param subscriber
         *      the {@link Subscriber} that will consume signals from this {@link SignalStream.Publisher}
         */
        void subscribe(Subscriber subscriber);

        boolean unsubscribe(Subscriber subscriber);
    }

    /**
     *  inlet for permits.
     *
     */
    public interface Subscriber {
        /**
         * asynchronous version of Semaphore.aquire()
         */
        void awake();
    }
}
