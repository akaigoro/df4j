package org.df4j.protocol;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscription;

/**
 * Flow of messages from Subscribers to Publishers
 * (as opposed to the flow of messages from Publishers to Subscribers in {{@link Flow}}).
 *
 * {@link ReverseFlow.Producer}s here still are active parties,
 * and {@link ReverseFlow.Consumer}s are passive, just like in {@link Flow}}.
        */
public class ReverseFlow {

    private ReverseFlow() {}

    /**
     * Produces messages.
     *
     * A {@link Producer} is a provider of a series of tokens, transmitting them to a {@link Consumer}(s).
     * <p>
     * @param <T>  type of messages.
     */
    public interface Producer<T> {

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
        void onSubscribe(ReverseFlowSubscription subscription);

        /**
         * {@link Consumer} gets data from {@link Producer}
         * when it has room to save the data.
         * @return the data from  {@link Producer}
         */
        T remove();

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

        void onError(Throwable e);
    }

    /**
     * Consumes messages.
     *
     * A {@link Consumer} can serve multiple {@link Producer}s subscribed dynamically
     * at various points in time.
     *
     * @param <T> the type of messages.
     */
    public interface Consumer<T> {

        /** subscribes producer to be notified  when Consumer is able to accept tokens
         * @param producer the {@link Producer} which offers messages for this {@link Consumer}
         */
        void feedFrom(Producer<T> producer);

        /**
         * @param publisher the {@link Publisher} which offers messages for this {@link Consumer}
         */
        void feedFrom(Publisher<T> publisher);
    }

    public interface ReverseFlowSubscription<T> extends Subscription {
        /**
         *
         * @param token
         * @return true if token accepted
         */
        boolean offer(T token);

        void onComplete();

        void onError(Throwable e);
    }
}
