package org.df4j.core.protocol;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

/**
 * One-shot multicast scalar protocol.
 * Fully interoperable with {@link CompletableFuture}: it is possible to call
 *   <pre><code>
 *   {@link ScalarMessage.Publisher#subscribe(CompletableFuture)} and
 *   {@link CompletableFuture}.whenComplete({@link ScalarMessage.Subscriber})
 * </code></pre>
 *
 */
public class ScalarMessage {

    private ScalarMessage() {}

    /**
     * A {@link Publisher} is a provider of a single element, publishing it to a {@link Subscriber}(s).
     * <p>
     * A {@link Publisher} can serve multiple {@link Subscriber}s subscribed dynamically
     * at various points in time.
     *
     * @param <T> the type of element signaled.
     */
    public interface Publisher<T> {

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

        default void subscribe(CompletableFuture<? super T> cf) {
            Subscriber<? super T> proxySubscriber = new CompletableFuture2ScalarSubscriber<>(cf);
            subscribe(proxySubscriber);
        }

        boolean unsubscribe(Subscriber<T> s);

    }

    /**
     * @param <T>  type of tokens
     */
    public interface Subscriber<T> extends Completion.CompletableObserver, BiConsumer<T, Throwable>
    {

        /**
         * Data notification sent by the {@link Publisher}
         *
         * @param t the element signaled
         */
        void onSuccess(T t);

        /**
         * Failed terminal state.
         * <p>
         * No further events will be sent.
         *
         * @param t the throwable signaled
         */
        default void onError(Throwable t) {}

        @Override
        default void accept(T t, Throwable throwable) {
            if (throwable == null) {
                onSuccess(t);
            } else {
                onError(throwable);
            }
        }
    }

    public static class CompletableFuture2ScalarSubscriber<T> implements Subscriber<T> {

        private final CompletableFuture<? super T> cf;

        public CompletableFuture2ScalarSubscriber(CompletableFuture<? super T> cf) {
            if (cf == null) {
                throw new NullPointerException();
            }
            this.cf = cf;
        }

        @Override
        public void onSuccess(T t) {
            cf.complete(t);
        }

        @Override
        public void onError(Throwable t) {
            cf.completeExceptionally(t);
        }
    }
}
