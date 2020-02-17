package org.df4j.core.port;

import org.df4j.core.dataflow.AsyncProc;
import org.df4j.protocol.Scalar;
import org.df4j.protocol.SimpleSubscription;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Output port for multiple scalar values.
 * Hos no internal memory for tokens. as becomes ready only when subscribers appear.
 * For demand-driven actors.
 *
 * @param <T> the type of completion value
 */
public class OutScalars<T> extends CompletablePort implements OutMessagePort<T>, Scalar.Source<T> {
    private final Lock plock = new ReentrantLock();
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
            plock.lock();
            try {
                subscriptions.add(subscription);
                unblock();
            } finally {
                plock.unlock();
            }
        }
    }

    public void onNext(T message) {
        ScalarSubscription subscription;
        plock.lock();
        try {
            subscription = subscriptions.remove();
            if (subscriptions.size() == 0) {
                block();
            }
        } finally {
            plock.unlock();
        }
        subscription.onNext(message);
    }

    protected void _onComplete(Throwable t) {
        Queue<ScalarSubscription> subscriptions;
        plock.lock();
        try {
            if (completed) {
                return;
            }
            completed = true;
            completionException = t;
            subscriptions = this.subscriptions;
            this.subscriptions = null;
        } finally {
            plock.unlock();
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
        private final Lock slock = new ReentrantLock();
        private Scalar.Observer<? super T> subscriber;
        private boolean cancelled;

        public ScalarSubscription(Scalar.Observer<? super T> subscriber) {
            this.subscriber = subscriber;
        }

        @Override
        public void cancel() {
            slock.lock();
            try {
                if (cancelled) {
                    return;
                }
                cancelled = true;
                subscriber = null;
                plock.lock();
                try {
                    subscriptions.remove(this);
                } finally {
                    plock.unlock();
                }
            } finally {
                slock.unlock();
            }
        }

        @Override
        public boolean isCancelled() {
            return cancelled;
        }

        public void onNext(T message) {
            Scalar.Observer<? super T> subs;
            slock.lock();
            try {
                if (cancelled) {
                    return;
                }
                cancelled = true;
                subs = subscriber;
                subscriber = null;
            } finally {
                slock.unlock();
            }
            subs.onSuccess(message);
        }

        private Scalar.Observer<? super T> removeSubscriber() {
            Scalar.Observer<? super T> subs;
            slock.lock();
            try {
                if (cancelled) {
                    return null;
                }
                cancelled = true;
                subs = subscriber;
                subscriber = null;
            } finally {
                slock.unlock();
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
