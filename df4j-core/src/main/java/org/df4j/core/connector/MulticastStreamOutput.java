package org.df4j.core.connector;

import org.df4j.core.Port;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.df4j.core.node.AsyncProc;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

/**
 * each input token it transferred to all subscribers
 *
 * @param <T> type of tokens
 */
public class MulticastStreamOutput<T> extends AsyncProc.Lock implements Port<T>, Publisher<T> {
    protected AsyncProc actor;
    protected Set<SimpleSubscription> subscriptions = new HashSet<>();

    public MulticastStreamOutput(AsyncProc actor, boolean blocked) {
        actor.super(blocked);
        this.actor = actor;
    }

    public MulticastStreamOutput(AsyncProc actor) {
        this(actor, false);
    }

    protected void subscribe(SimpleSubscription newSubscription) {
        subscriptions.add(newSubscription);
        newSubscription.subscriber.onSubscribe(newSubscription);
    }

    @Override
    public void subscribe(Subscriber<? super T> subscriber) {
        SimpleSubscription newSubscription = new SimpleSubscription(subscriber);
        subscribe(newSubscription);
    }

    private void forEachSubscription(Consumer<? super SimpleSubscription> operator) {
        synchronized (this) {
            if (subscriptions == null) {
                return; // completed already
            }
        }
        subscriptions.forEach(operator);
    }

    public void onNext(T item) {
        if (item == null) {
            throw new NullPointerException();
        }
        forEachSubscription((subscription) -> subscription.onNext(item));
    }

    @Override
    public void onError(Throwable throwable) {
        forEachSubscription((subscription) -> subscription.onError(throwable));
    }

    public synchronized void onComplete() {
        forEachSubscription(SimpleSubscription::onComplete);
        subscriptions = null;
        super.block();
    }

    public synchronized void cancel(SimpleSubscription subscription) {
        subscriptions.remove(subscription);
    }

    protected class SimpleSubscription implements Subscription, Port<T> {
        protected Subscriber<? super T> subscriber;

        public SimpleSubscription() {
        }

        public SimpleSubscription(Subscriber<? super T> subscriber) {
            this.subscriber = subscriber;
        }

        public synchronized void onNext(T message) {
            subscriber.onNext(message);
        }

        public synchronized void onError(Throwable throwable) {
            subscriber.onError(throwable);
            cancel();
        }

        /**
         * subscription closed by request of publisher
         * unregistering not needed
         */
        public synchronized void onComplete() {
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
            MulticastStreamOutput.this.cancel(this);
            subscriber = null;
        }
    }

}
