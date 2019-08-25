package org.df4j.core.protocols;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;
import java.util.function.BiConsumer;

public class Scalar {

    private Scalar() {}

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
         * This is a "factory method" and can be called multiple times, each time starting a new {@link Disposable}.
         * <p>
         * Each {@link Disposable} will work for only a single {@link Subscriber}.
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

        default void subscribe(Flow.Subscriber<T> subscriber) {
            Subscriber<T> proxySubscriber = new Flow2ScalarSubscriber<>(subscriber);
            subscribe(proxySubscriber);
        }

        default void subscribe(Flood.Subscriber<T> subscriber) {
            Subscriber<T> proxySubscriber = new Flood2ScalarSubscriber<>(subscriber);
            subscribe(proxySubscriber);
        }
    }

    /**
     * @param <T>  type of tokens
     */
    public interface Subscriber<T> extends BiConsumer<T, Throwable> {
        /**
         * Invoked after calling {@link Publisher#subscribe(Scalar.Subscriber)}.
         *
         * @param s
         *            {@link Disposable} that allows cancelling subscription via {@link Disposable#dispose()} }
         */
        default void onSubscribe(Disposable s) {}

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
        public void onSubscribe(Disposable s) {}

        @Override
        public void onSuccess(T t) {
            cf.complete(t);
        }

        @Override
        public void onError(Throwable t) {
            cf.completeExceptionally(t);
        }
    }

    public static class Flood2ScalarSubscriber<T> implements Subscriber<T>, Disposable {
        private final Flood.Subscriber<T> subscriber;
        private Disposable subscription;

        public Flood2ScalarSubscriber(Flood.Subscriber<T> subscriber) {
            this.subscriber = subscriber;
        }

        @Override
        public void onSubscribe(Disposable subscription) {
            this.subscription = subscription;
            subscriber.onSubscribe(this);
        }

        @Override
        public void onSuccess(T token) {
            subscriber.onNext(token);
            subscription.dispose();
        }

        @Override
        public void onError(Throwable t) {
            subscriber.onError(t);
        }

        @Override
        public void dispose() {
            subscription.dispose();
        }

        @Override
        public boolean isDisposed() {
            return subscription.isDisposed();
        }
    }

    public static class Flow2ScalarSubscriber<T> implements Subscriber<T>, Flow.Subscription {
        private final Flow.Subscriber<T> subscriber;
        private Disposable subscription;
        private boolean requested = false;
        private boolean done = false;
        private T token;

        public Flow2ScalarSubscriber(Flow.Subscriber<T> subscriber) {
            this.subscriber = subscriber;
        }

        @Override
        public void onSubscribe(Disposable subscription) {
            this.subscription = subscription;
            subscriber.onSubscribe(this);
        }

        @Override
        public synchronized void onSuccess(T token) {
            if (requested) {
                subscriber.onNext(token);
                subscription.dispose();
            } else {
                this.token = token;
                this.done = true;
            }
        }

        @Override
        public synchronized void onError(Throwable t) {
            subscriber.onError(t);
        }

        @Override
        public synchronized void request(long n) {
            requested = true;
            if (this.done) {
                subscriber.onNext(token);
                subscription.dispose();
            }
        }

        @Override
        public void cancel() {
            subscription.dispose();
        }
    }
}
