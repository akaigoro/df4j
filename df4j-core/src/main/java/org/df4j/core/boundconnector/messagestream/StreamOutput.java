package org.df4j.core.boundconnector.messagestream;

import org.df4j.core.boundconnector.Port;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.df4j.core.tasknode.AsyncProc;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

/**
 * serves multiple subscribers
 *
 * @param <T> type of tokens
 */
public class StreamOutput<T> extends AsyncProc.Lock implements Port<T>, Publisher<T> {
    protected AsyncProc actor;
    protected Set<SimpleSubscriptionImpl> subscriptions = new HashSet<>();

    public StreamOutput(AsyncProc actor, boolean blocked) {
        actor.super(blocked);
        this.actor = actor;
    }

    public StreamOutput(AsyncProc actor) {
        this(actor, false);
    }

    protected void subscribe(SimpleSubscriptionImpl newSubscription) {
        subscriptions.add(newSubscription);
        newSubscription.subscriber.onSubscribe(newSubscription);
    }

    @Override
    public void subscribe(Subscriber<? super T> subscriber) {
        SimpleSubscriptionImpl newSubscription = new SimpleSubscriptionImpl(subscriber);
        subscribe(newSubscription);
    }

    private void forEachSubscription(Consumer<? super SimpleSubscriptionImpl> operator) {
        synchronized (this) {
            if (subscriptions == null) {
                return; // completed already
            }
        }
        subscriptions.forEach(operator);
    }

    public void post(T item) {
        if (item == null) {
            throw new NullPointerException();
        }
        forEachSubscription((subscription) -> subscription.post(item));
    }

    @Override
    public void postFailure(Throwable throwable) {
        forEachSubscription((subscription) -> subscription.postFailure(throwable));
    }

    public synchronized void complete() {
        forEachSubscription(SimpleSubscriptionImpl::complete);
        subscriptions = null;
        super.turnOff();
    }

    public synchronized void cancel(SimpleSubscriptionImpl subscription) {
        subscriptions.remove(subscription);
    }

    protected class SimpleSubscriptionImpl implements Subscription, Port<T> {
        protected Subscriber<? super T> subscriber;
        public SimpleSubscriptionImpl(Subscriber<? super T> subscriber) {
            this.subscriber = subscriber;
        }

        public synchronized void post(T message) {
            subscriber.onNext(message);
        }

        public synchronized void postFailure(Throwable throwable) {
            subscriber.onError(throwable);
            cancel();
        }

        /**
         * subscription closed by request of publisher
         * unregistering not needed
         */
        public synchronized void complete() {
            if (subscriber == null) {
                return;
            }
            subscriber.onComplete();
            subscriber = null;
        }

        @Override
        public void request(long n) {}

        /**
         * subscription closed by request of subscriber
         */
        public synchronized void cancel() {
            if (subscriber == null) {
                return;
            }
            StreamOutput.this.cancel(this);
            subscriber = null;
        }
    }

}
