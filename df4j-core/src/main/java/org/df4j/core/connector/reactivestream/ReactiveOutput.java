package org.df4j.core.connector.reactivestream;

import org.df4j.core.connector.messagestream.StreamCollector;
import org.df4j.core.connector.permitstream.Semafor;
import org.df4j.core.node.AsyncTaskBase;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

/**
 * serves multiple subscribers
 * demonstrates usage of class AsyncTask.Semafor for handling back pressure
 *
 * An equivalent to java.util.concurrent.SubmissionPublisher
 *
 * @param <M>
 */
public class ReactiveOutput<M> extends AsyncTaskBase.Lock implements ReactivePublisher<M>, StreamCollector<M> {
    protected AsyncTaskBase base;
    protected Set<SimpleReactiveSubscriptionImpl> subscriptions = new HashSet<>();

    public ReactiveOutput(AsyncTaskBase base) {
        base.super(false);
        this.base = base;
    }

    @Override
    public <S extends ReactiveSubscriber<? super M>> S subscribe(S subscriber) {
        SimpleReactiveSubscriptionImpl newSubscription = new SimpleReactiveSubscriptionImpl(subscriber);
        subscriptions.add(newSubscription);
        subscriber.onSubscribe(newSubscription);
        return subscriber;
    }

    public synchronized void close() {
        subscriptions = null;
        super.turnOff();
    }

    public synchronized boolean closed() {
        return super.isBlocked();
    }

    public void forEachSubscription(Consumer<? super SimpleReactiveSubscriptionImpl> operator) {
        if (closed()) {
            return; // completed already
        }
        subscriptions.forEach(operator);
    }

    @Override
    public void post(M item) {
        forEachSubscription((subscription) -> subscription.post(item));
    }

    @Override
    public void postFailure(Throwable throwable) {
        forEachSubscription((subscription) -> subscription.postFailure(throwable));
    }

    public synchronized void complete() {
        forEachSubscription(SimpleReactiveSubscriptionImpl::complete);
    }

    class SimpleReactiveSubscriptionImpl extends Semafor implements ReactiveSubscription {
        protected ReactiveSubscriber<? super M> subscriber;
        private volatile boolean closed = false;

        public SimpleReactiveSubscriptionImpl(ReactiveSubscriber<? super M> subscriber) {
            super(base);
            if (subscriber == null) {
                throw new NullPointerException();
            }
            this.subscriber = subscriber;
        }

        public void post(M message) {
            if (isCompleted()) {
                throw new IllegalStateException("post to completed connector");
            }
            subscriber.post(message);
        }

        public void postFailure(Throwable throwable) {
            if (isCompleted()) {
                throw new IllegalStateException("postFailure to completed connector");
            }
            subscriber.postFailure(throwable);
            cancel();
        }

        /**
         * does nothing: counter decreases when a message is posted
         */
        @Override
        public void purge() {
        }

        /**
         * subscription closed by request of publisher
         * unregistering not needed
         */
        public void complete() {
            if (isCompleted()) {
                return;
            }
            subscriber.complete();
            subscriber = null;
        }

        private boolean isCompleted() {
            return subscriber == null;
        }

        /**
         * subscription closed by request of subscriber
         */
        public synchronized boolean cancel() {
            if (closed) {
                return false;
            }
            closed = true;
            subscriptions.remove(this);
            super.unRegister(); // and cannot be turned on
            return false;
        }

        @Override
        public void request(long n) {
            super.release(n);
        }
    }

}
