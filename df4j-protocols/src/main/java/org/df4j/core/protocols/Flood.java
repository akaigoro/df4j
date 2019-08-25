package org.df4j.core.protocols;

import java.util.concurrent.Flow;

public class Flood {

    private Flood() {} // uninstantiable


    /**
     * A producer of items (and related control messages) received by
     * Subscribers.  Each current {@link Subscriber} receives the same
     * items (via method {@code onNext}) in the same order, unless
     * drops or errors are encountered. If a Publisher encounters an
     * error that does not allow items to be issued to a Subscriber,
     * that Subscriber receives {@code onError}, and then receives no
     * further messages.  Otherwise, when it is known that no further
     * messages will be issued to it, a subscriber receives {@code
     * onComplete}.  Publishers ensure that Subscriber method
     * invocations for each subscription are strictly ordered in <a
     * href="package-summary.html#MemoryVisibility"><i>happens-before</i></a>
     * order.
     *
     * <p>Publishers may vary in policy about whether drops (failures
     * to issue an item because of resource limitations) are treated
     * as unrecoverable errors.  Publishers may also vary about
     * whether Subscribers receive items that were produced or
     * available before they subscribed.
     *
     * @param <T> the published item type
     */
    @FunctionalInterface
    public interface Publisher<T> {
        /**
         * Adds the given Subscriber if possible.  If already
         * subscribed, or the attempt to subscribe fails due to policy
         * violations or errors, the Subscriber's {@code onError}
         * method is invoked with an {@link IllegalStateException}.
         * Otherwise, the Subscriber's {@code onSubscribe} method is
         * invoked with a new {@link Flow.Subscription}.  Subscribers may
         * enable receiving items by invoking the {@code request}
         * method of this Subscription, and may unsubscribe by
         * invoking its {@code cancel} method.
         *
         * @param subscriber the subscriber
         * @throws NullPointerException if subscriber is null
         */
        void subscribe(Subscriber<? super T> subscriber);

        default void subscribe(Scalar.Subscriber<T> s) {
            Subscriber<? super T> proxySubscriber = new Scalar2FloodSubscriber<>(s);
            subscribe(proxySubscriber);
        }

    }

    /**
     * A receiver of messages.  The methods in this interface are
     * invoked in strict sequential order for each {@link
     * Disposable}.
     *
     * @param <T> the subscribed item type
     */
    public interface Subscriber<T> {

        /**
         * Method invoked with a Subscription's next item.  If this
         * method throws an exception, resulting behavior is not
         * guaranteed, but may cause the Subscription to be cancelled.
         *
         * @param item the item
         */
        void onNext(T item);

        /**
         * Method invoked when it is known that no additional
         * Subscriber method invocations will occur for a Subscription
         * that is not already terminated by error, after which no
         * other Subscriber methods are invoked by the Subscription.
         * If this method throws an exception, resulting behavior is
         * undefined.
         */
        void onComplete();

        /**
         * Invoked after calling {@link Scalar.Publisher#subscribe(Scalar.Subscriber)}.
         *
         * @param s
         *            {@link Disposable} that allows cancelling subscription via {@link Disposable#dispose()} }
         */
        default void onSubscribe(Disposable s) {}

        /**
         * Failed terminal state.
         * <p>
         * No further events will be sent.
         *
         * @param t the throwable signaled
         */
        default void onError(Throwable t) {}
    }

    /**
     *
     * converts Scalar.Subscriber to a Flood.Subscriber
     *
     * @param <T> type of tokens
     */
    public static class Scalar2FloodSubscriber<T> implements Subscriber<T>, Disposable {
        private final Scalar.Subscriber<T> subscriber;
        private Disposable subscription;

        Scalar2FloodSubscriber(Scalar.Subscriber<T> subscriber) {
            this.subscriber = subscriber;
        }

        @Override
        public void onSubscribe(Disposable s) {
            subscription = s;
            subscriber.onSubscribe(this);
        }

        @Override
        public void onError(Throwable t) {
            subscriber.onError(t);
        }

        @Override
        public void onComplete() {
            subscriber.onSuccess(null);
            subscription.dispose();
        }

        @Override
        public void onNext(T token) {
            subscriber.onSuccess(token);
            subscription.dispose();
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
}
