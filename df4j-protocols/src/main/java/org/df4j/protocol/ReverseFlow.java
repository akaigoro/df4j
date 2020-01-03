package org.df4j.protocol;

/**
 * Flow of messages from Subscribers to Publishers
 * (as opposed to the flow of messages from Publishers to Subscribers in {{@link Flow}}).
 *
 * {@link ReverseFlow.Subscriber}s here still are active parties,
 * and {@link ReverseFlow.Publisher}s are passive, just like in {@link Flow}}.
        */
public class ReverseFlow {

    private ReverseFlow() {}

    /**
     * Consumes messages.
     *
     * A {@link Publisher} can serve multiple {@link Subscriber}s subscribed dynamically
     * at various points in time.
     *
     * @param <T> the type of messages.
     */
    public interface Publisher<T> {

        /**
         * @param producer the {@link Subscriber} which offers messages for this {@link Publisher}
         */
        void subscribe(Subscriber<T> producer);
    }

    /**
     * Produces messages.
     *
     * A {@link Subscriber} is a provider of a series of tokens, transmitting them to a {@link Publisher}(s).
     * <p>
     * @param <T>  type of messages.
     */
    public interface Subscriber<T> {

        /**
         *
         * @return true if producer has completed the flow of items
         */
        default boolean isCompleted() {
            return false;
        }

        default Throwable getCompletionException()  {
            return null;
        }

        /**
         * Method invoked prior to invoking any other Subscriber
         * methods for the given Subscription. If this method throws
         * an exception, resulting behavior is not guaranteed, but may
         * cause the Subscription not to be established or to be cancelled.
         *
         * <p>Typically, implementations of this method invoke {@code
         * subscription.request} to enable sending items.
         *
         * @param subscription a new subscription
         */
        void onSubscribe(Subscription subscription);

        /**
         * {@link Publisher} gets data from {@link Subscriber}
         * when it has room to save the data.
         * @return the data from  {@link Subscriber}
         */
        T remove();

        /**
         * called by {@link Publisher} when it is completed and asks to not disturb.
         *
         */
        void cancel();
    }

    /**
     * A {@link Subscription} represents a one-to-one lifecycle of a {@link Subscriber} subscribing to a {@link Publisher}.
     * <p>
     * It can only be used once by a single {@link Subscriber}.
     * <p>
     * It is used to both signal desire for data and cancel demand (and allow resource cleanup).
     *
     */
    public static interface Subscription extends Scalar.Subscription {
        /**
         * No events will be sent by a {@link Publisher} until demand is signaled via this method.
         * <p>
         * It can be called however often and whenever needed—but the outstanding cumulative demand must never exceed Long.MAX_VALUE.
         * An outstanding cumulative demand of Long.MAX_VALUE may be treated by the {@link Publisher} as "effectively unbounded".
         * <p>
         * Whatever has been requested can be sent by the {@link Publisher} so only signal demand for what can be safely handled.
         * <p>
         * A {@link Publisher} can send less than is requested if the stream ends but
         * then must emit either {@link Subscriber#onError(Throwable)} or {@link Subscriber#onComplete()}.
         *
         * @param n the strictly positive number of elements to requests to the upstream {@link Publisher}
         */
        public void request(long n);
    }
}
