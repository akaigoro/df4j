package org.df4j.core.boundconnector.messagestream;

import org.df4j.core.tasknode.AsyncProc;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

/**
 * serves multiple subscribers
 *
 * @param <M> type of tokens
 */
public class StreamOutput<M> extends AsyncProc.Lock implements Publisher<M>, Subscriber<M> {
    protected AsyncProc actor;
    protected Subscription subscription;
    protected Set<SimpleSubscriptionImpl> subscriptions = new HashSet<>();

    public StreamOutput(AsyncProc actor) {
        actor.super(false);
        this.actor = actor;
    }

    @Override
    public void subscribe(Subscriber<? super M> subscriber) {
        SimpleSubscriptionImpl newSubscription = new SimpleSubscriptionImpl(subscriber);
        subscriptions.add(newSubscription);
        subscriber.onSubscribe(newSubscription);
    }

    public synchronized void close() {
        if (subscription != null) {
            subscription.cancel();
            subscription = null;
        }
        onComplete();
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
    public void onSubscribe(Subscription subscription) {
        this.subscription = subscription;
    }

    @Override
    public void onNext(M item) {
        if (item == null) {
            throw new NullPointerException();
        }
        forEachSubscription((subscription) -> subscription.post(item));
    }

    @Override
    public void onError(Throwable throwable) {
        forEachSubscription((subscription) -> subscription.postFailure(throwable));
    }

    @Override
    public synchronized void onComplete() {
        forEachSubscription(SimpleSubscriptionImpl::complete);
    }

    class SimpleSubscriptionImpl implements Subscription {
        protected Subscriber<? super M> subscriber;
        private volatile boolean closed = false;

        public SimpleSubscriptionImpl(Subscriber<? super M> subscriber) {
            this.subscriber = subscriber;
        }

        public void post(M message) {
            subscriber.onNext(message);
        }

        public void postFailure(Throwable throwable) {
            subscriber.onError(throwable);
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
            subscriber.onComplete();
            subscriber = null;
        }

        @Override
        public void request(long n) {
        }

        /**
         * subscription closed by request of subscriber
         */
        public void cancel() {
            synchronized(StreamOutput.this) {
                if (closed) {
                    return;
                }
                closed = true;
                subscriptions.remove(this);
            }
        }
    }

}
