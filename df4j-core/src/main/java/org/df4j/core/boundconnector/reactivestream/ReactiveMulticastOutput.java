package org.df4j.core.boundconnector.reactivestream;

import org.df4j.core.boundconnector.Port;
import org.df4j.core.tasknode.AsyncProc;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * serves multiple subscribers
 *
 * @param <T> the type of broadcasted values
 */
public class ReactiveMulticastOutput<T> extends AsyncProc.Lock implements Port<T>, Publisher<T> {
    protected ValuePublisher currentValuePublisher = new ValuePublisher();

    public ReactiveMulticastOutput(AsyncProc actor) {
        actor.super(true);
    }

    @Override
    public synchronized void subscribe(Subscriber<? super T> subscriber) {
        currentValuePublisher.subscribe(subscriber);
        turnOn();
    }

    protected synchronized boolean isCompleted() {
        return currentValuePublisher.state == ValueState.STREAM_COMPLETED
                || currentValuePublisher.state == ValueState.STREAM_FAILED;
    }

    @Override
    public synchronized void post(T value) {
        if (isCompleted()) {
            throw new IllegalStateException("completed already");
        }
        if (value == null) {
            throw new IllegalArgumentException();
        }
        currentValuePublisher.post(value);
    }

    @Override
    public synchronized void postFailure(Throwable throwable) {
        if (isCompleted()) {
            throw new IllegalStateException("completed already");
        }
        super.turnOff();
        ValuePublisher currentSubscriptionsLoc = currentValuePublisher;
        currentSubscriptionsLoc.postFailure(throwable);
    }

    public synchronized void complete() {
        if (isCompleted()) {
            throw new IllegalStateException("completed already");
        }
        super.turnOff();
        ValuePublisher currentSubscriptionsLoc = currentValuePublisher;
        currentSubscriptionsLoc.complete();
    }

    enum ValueState {
        VALUE_NOT_READY,
        VALUE_READY,
        STREAM_COMPLETED,
        STREAM_FAILED
    }

    class ValuePublisher implements Port<T>, Publisher<T> {
        volatile ValueState state = ValueState.VALUE_NOT_READY;
        volatile T value = null;
        volatile Throwable throwable = null;
        ValuePublisher next = null;
        Set<MulticastReactiveSubscription> subscriptions = new HashSet<>();

        public void subscribe(Subscriber<? super T> subscriber) {
            MulticastReactiveSubscription newSubscription = new MulticastReactiveSubscription(this, subscriber);
            subscriber.onSubscribe(newSubscription);
        }

        public void post(T value) {
            this.value = value;
            this.state = ValueState.VALUE_READY;
            ReactiveMulticastOutput.this.currentValuePublisher = this.next = new ValuePublisher();
            Iterator<MulticastReactiveSubscription> it = subscriptions.iterator();
            while (it.hasNext()) {
                MulticastReactiveSubscription subscription = it.next();
                if (subscription.requested > 0) {
                    subscription.requested--;
                    subscription.parent = next;
                    next.subscriptions.add(subscription);
                    it.remove();
                    subscription.subscriber.onNext(value);
                }
            }
        }

        public void postFailure(Throwable throwable) {
            if (throwable == null) {
                throw new IllegalArgumentException();
            }
            this.throwable = throwable;
            this.state = ValueState.STREAM_FAILED;
            Iterator<MulticastReactiveSubscription> it = subscriptions.iterator();
            while (it.hasNext()) {
                MulticastReactiveSubscription subscription = it.next();
                subscription.parent = null;
                subscription.subscriber.onError(throwable);
            }
            subscriptions = null;
        }

        public void complete() {
            this.state = ValueState.STREAM_COMPLETED;
            Iterator<MulticastReactiveSubscription> it = subscriptions.iterator();
            while (it.hasNext()) {
                MulticastReactiveSubscription subscription = it.next();
                subscription.parent = null;
                subscription.subscriber.onComplete();
            }
            subscriptions = null;
        }
    }

    class MulticastReactiveSubscription implements Subscription {
        protected ValuePublisher parent;
        protected Subscriber<? super T> subscriber;
        private long requested = 0;

        public MulticastReactiveSubscription(ValuePublisher parent, Subscriber<? super T> subscriber) {
            this.parent = parent;
            this.subscriber = subscriber;
            parent.subscriptions.add(this);
        }

        @Override
        public void request(long n) {
            synchronized(ReactiveMulticastOutput.this) {
                if (n <= 0){
                    subscriber.onError(new IllegalArgumentException());
                    return;
                }
                requested += n;
                ValuePublisher currentParent = parent;
                loop:
                while (requested > 0) {
                    if (currentParent == null) {
                        break;
                    }
                    switch (currentParent.state) {
                        case VALUE_NOT_READY:
                            break loop;
                        case VALUE_READY:
                            subscriber.onNext(currentParent.value);
                            requested--;
                            currentParent = parent.next;
                            break;
                        case STREAM_COMPLETED:
                            subscriber.onComplete();
                            break loop;
                        case STREAM_FAILED:
                            subscriber.onError(currentParent.throwable);
                            break loop;
                    }
                }
                if (currentParent != parent) {
                    parent.subscriptions.remove(this);
                    parent = currentParent;
                    if (parent != null && parent.subscriptions != null) {
                        parent.subscriptions.add(this);
                    }
                }
            }
        }

        /**
         * subscription closed by request of subscriber
         */
        public void cancel() {
            synchronized(ReactiveMulticastOutput.this) {
                if (subscriber == null) {
                    return;
                }
                if (parent != null && parent.subscriptions != null) {
                    parent.subscriptions.remove(this);
                }
                subscriber = null;
                parent = null;
            }
        }
    }

}
