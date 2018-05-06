package org.df4j.core.core.connector.messagestream;

import org.df4j.core.core.connector.messagescalar.SimpleSubscription;
import org.df4j.core.core.node.Actor;
import org.df4j.core.core.node.AsyncTask.Connector;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

/**
 * serves multiple subscribers
 * demonstrates usage of class Actor.Semafor for handling back pressure
 *
 * @param <M>
 */
public class StreamOutput<M> extends Connector implements StreamPublisher<M>, StreamCollector<M> {
    protected Actor base;
    protected Set<SimpleSubscriptionImpl> subscriptions = new HashSet<>();

    public StreamOutput(Actor base) {
        base.super(false);
        this.base = base;
    }

    @Override
    public <S extends StreamSubscriber<? super M>> S subscribe(S subscriber) {
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

    @Override
    public synchronized void complete() {
        forEachSubscription(SimpleSubscriptionImpl::complete);
    }

    class SimpleSubscriptionImpl implements SimpleSubscription {
        protected StreamSubscriber<? super M> subscriber;
        private volatile boolean closed = false;

        public SimpleSubscriptionImpl(StreamSubscriber<? super M> subscriber) {
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
         * subscription closed by request of publisher
         * unregistering not needed
         */
        public void complete() {
            if (subscriber == null) {
                return;
            }
            subscriber.complete();
            subscriber = null;
        }

        /**
         * subscription closed by request of subscriber
         */
        public boolean cancel() {
            synchronized(StreamOutput.this) {
                if (closed) {
                    return false;
                }
                closed = true;
                subscriptions.remove(this);
                return false;
            }
        }
    }

}
