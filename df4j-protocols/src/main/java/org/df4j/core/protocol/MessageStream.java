package org.df4j.core.protocol;

import java.util.concurrent.CompletableFuture;

public class MessageStream {

    private MessageStream() {}

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
         * A {@link Subscriber} should only subscribe once to a single {@link Publisher}.
         * <p>
         * If the {@link Publisher} rejects the subscription attempt or otherwise fails it will
         * signal the error via {@link Subscriber#onComplete}.
         *
         * @param subscriber the {@link Subscriber} that will consume signals from this {@link Publisher}
         */
        void subscribe(Subscriber<T> subscriber);

        default Subscriber<T> subscribe(ScalarMessage.Subscriber<T> cf) {
            Subscriber<T> proxySubscriber = new ScalarToStreamSubscriber<>(cf);
            subscribe(proxySubscriber);
            return proxySubscriber;
        }

        default Subscriber<T> subscribe(CompletableFuture<? super T> cf) {
            Subscriber<T> proxySubscriber = new CompletableFuture2ScalarSubscriber<>(cf);
            subscribe(proxySubscriber);
            return proxySubscriber;
        }

        boolean unsubscribe(Subscriber<T> subscriber);
    }

    /**
     * by extending {@link ScalarMessage.Subscriber}, {@link MessageStream.Subscriber} can subscribe to {@link ScalarMessage.Publisher}.
     * @param <T>  type of tokens
     */
    public interface Subscriber<T> extends ScalarMessage.Subscriber<T> {
        /**
         * Data notification sent by the {@link Publisher}
         *
         * @param t the element signaled
         */
        void onNext(T t);

        /**
         * Data notification sent by the {@link Publisher}
         *
         * @param t the element signaled
         */
        default void onSuccess(T t) {
            onNext(t);
            onComplete();
        }
    }

    public static class ScalarToStreamSubscriber<T> implements Subscriber<T> {

        private final ScalarMessage.Subscriber<? super T> scalarSub;

        public ScalarToStreamSubscriber(ScalarMessage.Subscriber<? super T> scalarSub) {
            if (scalarSub == null) {
                throw new NullPointerException();
            }
            this.scalarSub = scalarSub;
        }

        @Override
        public void onNext(T t) {
            scalarSub.onSuccess(t);
        }

        @Override
        public void onComplete() {
            scalarSub.onSuccess(null);
        }

        @Override
        public void onError(Throwable t) {
            scalarSub.onError(t);
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
        public void onNext(T t) {
            cf.complete(t);
        }

        @Override
        public void onComplete() {
            cf.complete(null);
        }

        @Override
        public void onError(Throwable t) {
            cf.completeExceptionally(t);
        }
    }
}
