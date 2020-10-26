package org.df4j.core.port;

import org.df4j.core.actor.AsyncProc;
import org.df4j.protocol.OutMessagePort;
import org.df4j.protocol.Scalar;
import org.df4j.protocol.SimpleSubscription;

import java.util.LinkedList;
import java.util.Queue;

/**
 * Output port for multiple scalar values.
 * Hos no internal memory for tokens. as becomes ready only when subscribers appear.
 * For demand-driven actors.
 *
 * @param <T> the type of completion value
 */
public class OutScalars<T> extends CompletablePort implements OutMessagePort<T>, Scalar.Publisher<T> {
    private  Queue<ScalarSubscription> subscriptions = new LinkedList<>();

    public OutScalars(AsyncProc parent) {
        super(parent);
    }

    @Override
    public void subscribe(Scalar.Subscriber<? super T> subscriber) {
        ScalarSubscription subscription = new ScalarSubscription(subscriber);
        subscriber.onSubscribe(subscription);
        if (completed) {
            subscription.onComplete(completionException);
        } else {
            synchronized(transition) {
                subscriptions.add(subscription);
                unblock();
            }
        }
    }

    public void onNext(T message) {
        ScalarSubscription subscription;
        synchronized(transition) {
            subscription = subscriptions.remove();
            if (subscriptions.size() == 0) {
                block();
            }
        }
        subscription.onNext(message);
    }

    protected void _onComplete(Throwable t) {
        Queue<ScalarSubscription> subscriptions;
        synchronized(transition) {
            if (completed) {
                return;
            }
            completed = true;
            completionException = t;
            subscriptions = this.subscriptions;
            this.subscriptions = null;
        }
        for (;;) {
            ScalarSubscription subscription = subscriptions.poll();
            if (subscription == null) {
                break;
            }
            subscription.onComplete(t);
        }
    }

    class ScalarSubscription implements SimpleSubscription {
        private Scalar.Subscriber<? super T> subscriber;
        private boolean cancelled;

        public ScalarSubscription(Scalar.Subscriber<? super T> subscriber) {
            this.subscriber = subscriber;
        }

        @Override
        public void cancel() {
            synchronized(transition) {
                if (cancelled) {
                    return;
                }
                cancelled = true;
                subscriber = null;
                subscriptions.remove(this);
            }
        }

        @Override
        public boolean isCancelled() {
            return cancelled;
        }

        public void onNext(T message) {
            Scalar.Subscriber<? super T> subs;
            synchronized(transition) {
                if (cancelled) {
                    return;
                }
                cancelled = true;
                subs = subscriber;
                subscriber = null;
            }
            subs.onSuccess(message);
        }

        private Scalar.Subscriber<? super T> removeSubscriber() {
            Scalar.Subscriber<? super T> subs;
            synchronized(transition) {
                if (cancelled) {
                    return null;
                }
                cancelled = true;
                subs = subscriber;
                subscriber = null;
            }
            return subs;
        }

        void onComplete(Throwable completionException) {
            Scalar.Subscriber<? super T> subscriber = removeSubscriber();
            if (subscriber == null) {
                return;
            }
            if (completionException == null) {
                subscriber.onComplete();
            } else {
                subscriber.onError(completionException);
            }
        }
    }
}
