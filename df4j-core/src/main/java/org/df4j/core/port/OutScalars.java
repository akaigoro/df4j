package org.df4j.core.port;

import org.df4j.core.dataflow.AsyncProc;
import org.df4j.protocol.Scalar;
import org.reactivestreams.Subscription;

import java.util.LinkedList;
import java.util.Queue;

/**
 * Output port for multiple scalar values.
 * Has no internal memory for tokens.
 * Becomes ready only when subscribers appear.
 * For demand-driven actors.
 *
 * @param <T> the type of completion value
 */
public class OutScalars<T> extends CompletablePort implements OutMessagePort<T>, Scalar.Source<T> {
    private  Queue<ScalarSubscription> subscriptions = new LinkedList<>();

    public OutScalars(AsyncProc parent) {
        super(parent);
    }

    @Override
    public void subscribe(Scalar.Observer<? super T> subscriber) {
        ScalarSubscription subscription = new ScalarSubscription(subscriber);
        subscriber.onSubscribe(subscription);
        if (completed) {
            subscription.onComplete(completionException);
        } else {
            synchronized(parent) {
                subscriptions.add(subscription);
                unblock();
            }
        }
    }

    public void onNext(T message) {
        ScalarSubscription subscription;
        synchronized(parent) {
            subscription = subscriptions.remove();
            if (subscriptions.size() == 0) {
                block();
            }
        }
        subscription.onNext(message);
    }

    protected void _onComplete(Throwable t) {
        Queue<ScalarSubscription> subscriptions;
        synchronized(parent) {
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

    class ScalarSubscription implements Subscription {
        private Scalar.Observer<? super T> subscriber;
        private boolean cancelled;

        public ScalarSubscription(Scalar.Observer<? super T> subscriber) {
            this.subscriber = subscriber;
        }

        @Override
        public void request(long n) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void cancel() {
            synchronized(parent) {
                if (cancelled) {
                    return;
                }
                cancelled = true;
                subscriber = null;
                subscriptions.remove(this);
            }
        }

        public void onNext(T message) {
            Scalar.Observer<? super T> subs;
            synchronized(parent) {
                if (cancelled) {
                    return;
                }
                cancelled = true;
                subs = subscriber;
                subscriber = null;
            }
            subs.onSuccess(message);
        }

        private Scalar.Observer<? super T> removeSubscriber() {
            Scalar.Observer<? super T> subs;
            synchronized(parent) {
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
            Scalar.Observer<? super T> subscriber = removeSubscriber();
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
