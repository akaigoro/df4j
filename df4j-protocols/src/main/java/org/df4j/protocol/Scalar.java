package org.df4j.protocol;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

/**
 * One-shot multicast message protocol.
 */
public class Scalar {


    private Scalar() {}

    /**
     * A {@link Source} is a provider of a single element, publishing it to a {@link Observer}(s).
     * <p>
     * A {@link Source} can serve multiple {@link Observer}s subscribed dynamically
     * at various points in time.
     *
     * @param <T> the type of element signaled.
     */
    public interface Source<T> {

        /**
         * Request {@link Source} to start scalar data.
         * <p>
         * This is a "factory method" and can be called multiple times.
         * <p>
         * A {@link Observer} should only subscribe once to a single {@link Source}.
         * <p>
         * If the {@link Source} rejects the subscription attempt or otherwise fails it will
         * signal the error via {@link Observer#onError}.
         *
         * @param s the {@link Observer} that will consume signals from this {@link Source}
         */
        void subscribe(Observer<? super T> s);
    }

    /**
     * Supposed to receive single message per subscription.
     * To receive more messages, another subscription is required.
     * Subscription to the same {@link Scalar.Source} is useless - the same result woould be received.
     *
     * @param <T>  type of tokens
     */
    public interface Observer<T> extends BiConsumer<T, Throwable> {
        void onSubscribe(SimpleSubscription subscription);

        /**
         * Data notification sent by the {@link Source}
         *
         * @param t the element signaled
         */
        void onSuccess(T t);

        default void onComplete() {
            onError(null);
        }

        /**
         * Failed terminal state.
         * <p>
         * No further events will be sent.
         *
         * @param t the throwable signaled
         */
        default void onError(Throwable t) {}

        /**
         * allows to subscribe to {@link CompletableFuture}
         *
         * @param t token value, or null if exception
         * @param throwable exception, if any
         */
        @Override
        default void accept(T t, Throwable throwable) {
            if (throwable == null) {
                onSuccess(t);
            } else {
                onError(throwable);
            }
        }
    }

}
