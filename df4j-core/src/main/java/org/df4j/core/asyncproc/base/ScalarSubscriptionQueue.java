package org.df4j.core.asyncproc.base;

import org.df4j.core.protocols.Disposable;
import org.df4j.core.protocols.Scalar;
import org.df4j.core.util.SubscriptionCancelledException;
import org.df4j.core.util.linked.Link;
import org.df4j.core.util.linked.LinkedQueue;

/**
 * subscribers can be scalar subscribers, stream subscribers, and CompletableFutures.
 *
 * @param <T>
 */
public class ScalarSubscriptionQueue<T> extends LinkedQueue<ScalarSubscriptionQueue<T>.ScalarSubscriptionImpl> implements Scalar.Publisher<T> {

    @Override
    public void subscribe(Scalar.Subscriber<? super T> subscriber) {
        ScalarSubscriptionImpl subscription = new ScalarSubscriptionImpl(subscriber);
        offer(subscription);
        subscriber.onSubscribe(subscription);
    }

    public void onSuccess(T value) {
        ScalarSubscriptionImpl subscription = poll();
        for (; subscription != null; subscription = poll()) {
            try {
                subscription.onSuccess(value);
            } catch (SubscriptionCancelledException e) {
            }
        }
    }

    public void onError(Throwable ex) {
        ScalarSubscriptionImpl subscription = poll();
        for (; subscription != null; subscription = poll()) {
            try {
                subscription.onError(ex);
            } catch (SubscriptionCancelledException e) {
            }
        }
    }

    public class ScalarSubscriptionImpl extends Link<ScalarSubscriptionImpl> implements Disposable {
        protected Scalar.Subscriber subscriber;

        public ScalarSubscriptionImpl(Scalar.Subscriber subscriber) {
            if (subscriber == null) {
                throw new NullPointerException();
            }
            this.subscriber = subscriber;
        }

        public synchronized boolean isDisposed() {
            return subscriber == null;
        }

        protected void unlink() {
            super.unlink();
        }

        public void dispose() {
            synchronized(this){
                if (isDisposed()) {
                    return;
                }
                subscriber = null;
            }
            ScalarSubscriptionQueue.this.remove(this);
        }

        public synchronized Scalar.Subscriber extractScalarSubscriber() throws SubscriptionCancelledException {
            if (isDisposed()) {
                throw new SubscriptionCancelledException();
            } else {
                Scalar.Subscriber subscriberLoc = subscriber;
                subscriber = null;
                return subscriberLoc;
            }
        }

        public <T> void onSuccess(T value) throws SubscriptionCancelledException {
            extractScalarSubscriber().onSuccess(value);
        }

        public void onError(Throwable t) throws SubscriptionCancelledException {
            extractScalarSubscriber().onError(t);
        }

    }
}