package org.df4j.protocol;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

/**
 * One-shot multicast message protocol.
 */
public class Scalar {


    private Scalar() {}

    /**
     * Synchronous interface
     * @param <T> the type of element signaled.
     */
    public interface Source<T> {

        boolean isCompleted();

        T get();
    }

    /**
     * Asynchronous interface
     * @param <T> the type of element signaled.
     */
    public interface Publisher<T> {

        boolean isCompleted();

        /**
         * Request {@link Publisher} to start scalar data.
         * <p>
         * This is a "factory method" and can be called multiple times.
         * <p>
         * A {@link Subscriber} should only subscribe once to a single {@link Publisher}.
         * <p>
         * If the {@link Publisher} rejects the subscription attempt or otherwise fails it will
         * signal the error via {@link Subscriber#onError}.
         *
         * @param s the {@link Subscriber} that will consume signals from this {@link Publisher}
         */
        void subscribe(Subscriber<? super T> s);
    }

    /**
     * Supposed to receive single message per subscription.
     * To receive more messages, another subscription is required.
     * Subscription to the same {@link Publisher} is useless - the same result woould be received.
     *
     * @param <T>  type of tokens
     */
    public interface Subscriber<T> extends Completable.Subscriber, BiConsumer<T, Throwable> {
        void onSubscribe(SimpleSubscription subscription);

        /**
         * Data notification sent by the {@link Publisher}
         *
         * @param t the element signaled
         */
        void onSuccess(T t);

        void onComplete();

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
