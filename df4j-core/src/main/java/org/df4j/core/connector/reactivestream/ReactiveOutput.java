package org.df4j.core.connector.reactivestream;

import org.df4j.core.connector.messagestream.StreamCollector;
import org.df4j.core.connector.permitstream.Semafor;
import org.df4j.core.node.Actor;
import org.df4j.core.node.AsyncTask;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

/**
 * serves multiple subscribers
 * demonstrates usage of class Actor.Semafor for handling back pressure
 *
 * An equivalent to java.util.concurrent.SubmissionPublisher
 *
 * @param <M>
 */
public class ReactiveOutput<M> extends AsyncTask.Connector implements Publisher<M>, StreamCollector<M> {
    protected Actor base;
    protected Set<SimpleSubscriptionImpl> subscriptions = new HashSet<>();

    public ReactiveOutput(Actor base) {
        base.super(false);
        this.base = base;
    }

    @Override
    public <S extends Subscriber<? super M>> S subscribe(S subscriber) {
        SimpleSubscriptionImpl newSubscription = new SimpleSubscriptionImpl(subscriber);
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

    public void forEachSubscription(Consumer<? super SimpleSubscriptionImpl> operator) {
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
        forEachSubscription(SimpleSubscriptionImpl::complete);
    }

    class SimpleSubscriptionImpl extends Semafor implements Subscription {
        protected Subscriber<? super M> subscriber;
        private volatile boolean closed = false;

        public SimpleSubscriptionImpl(Subscriber<? super M> subscriber) {
            super(base);
            this.subscriber = subscriber;
        }

        public void post(M message) {
            subscriber.post(message);
        }

        public void postFailure(Throwable throwable) {
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
            subscriber.complete();
            subscriber = null;
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
