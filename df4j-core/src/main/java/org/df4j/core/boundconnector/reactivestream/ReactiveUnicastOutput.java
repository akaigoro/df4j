package org.df4j.core.boundconnector.reactivestream;

import org.df4j.core.boundconnector.Port;
import org.df4j.core.boundconnector.messagestream.UnicastStreamOutput;
import org.df4j.core.tasknode.AsyncProc;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

import java.util.ArrayDeque;
import java.util.Queue;

/**
 * serves multiple subscribers
 * demonstrates usage of class {@link org.df4j.core.boundconnector.permitstream.Semafor} for handling back pressure
 *
 * Each message is routed to only one subscriber.
 *
 * @param <T> the type of broadcasted values
 */
public class ReactiveUnicastOutput<T> extends UnicastStreamOutput<T> implements Publisher<T> {
    protected Queue<UnicastReactiveSubscription> activeSubscriptions = new ArrayDeque<>();
    boolean completed = false;

    public ReactiveUnicastOutput(AsyncProc actor) {
        super(actor, true);
    }

    protected Port<T> currentSubscription() {
        return activeSubscriptions.peek();
    }

    @Override
    public synchronized void subscribe(Subscriber<? super T> subscriber) {
        UnicastReactiveSubscription newSubscription = new UnicastReactiveSubscription(subscriber);
        subscriptions.add(newSubscription);
        super.subscribe(newSubscription);
    }

    public synchronized boolean completed() {
        return completed;
    }

    public synchronized void post(T item) {
        currentSubscription().post(item);
    }

    public synchronized void complete() {
        if (completed) {
            return; // completed already
        }
        subscriptions.forEach((sub)->sub.complete());
        completed = true;
        super.turnOff();
    }

    public void postFailure(Throwable throwable) {
        currentSubscription().postFailure(throwable);
    }

    protected synchronized void activeSubscriptionsAdd(UnicastReactiveSubscription subscription) {
        boolean wasEmpty = activeSubscriptions.isEmpty();
        activeSubscriptions.add(subscription);
        if (wasEmpty) {
            super.turnOn();
        }
    }

    protected synchronized void activeSubscriptionsRemove() {
        activeSubscriptions.remove();
        if (activeSubscriptions.isEmpty()) {
            super.turnOff();
        }
    }

    public synchronized void cancel(SimpleSubscription subscription) {
        super.cancel(subscription);
        activeSubscriptions.remove(subscription);
        if (activeSubscriptions.isEmpty()) {
            super.turnOff();
        }
    }

    class UnicastReactiveSubscription extends SimpleSubscription {
        private volatile boolean completed = false;
        private long requested = 0;

        public UnicastReactiveSubscription(Subscriber<? super T> subscriber) {
            super(subscriber);
        }

        public synchronized void post(T message) {
            if (completed) {
                throw new IllegalStateException("post to completed connector");
            }
            if (requested == 0) {
                throw new IllegalStateException("not requested");
            }
            requested--;
            if (requested == 0) {
                activeSubscriptionsRemove();
            }
            super.post(message);
        }

        public synchronized void postFailure(Throwable throwable) {
            if (subscriber == null) {
                throw new IllegalStateException("onError to completed connector");
            }
            super.postFailure(throwable);
        }

        @Override
        public synchronized void request(long n) {
            if (n <= 0){
                subscriber.onError(new IllegalArgumentException());
                return;
            }
            boolean wasPassive = requested == 0;
            requested += n;
            if (wasPassive) {
                activeSubscriptionsAdd(this);
            }
        }
    }

}
