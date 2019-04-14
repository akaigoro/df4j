package org.df4j.core.asyncproc;

import org.df4j.core.util.linked.Link;
import org.df4j.core.util.linked.LinkedQueue;

public class ScalarSubscriptionQueue<T> extends LinkedQueue<ScalarSubscriptionQueue.ScalarSubscription<T>> implements ScalarPublisher<T> {

    public void subscribe(ScalarSubscriber<? super T> s) {
        ScalarSubscription subscription = new ScalarSubscription(this, s);
        synchronized (this) {
            add(subscription);
        }
        s.onSubscribe(subscription);
        subscription.setInitialized();
    }

    public void onComplete(T value) {
        ScalarSubscription subscription = poll();
        for (; subscription != null; subscription = poll()) {
            subscription.onComplete(value);
        }
    }

    public void onError(Throwable ex) {
        ScalarSubscription subscription = poll();
        for (; subscription != null; subscription = poll()) {
            subscription.onError(ex);
        }
    }

    public static class ScalarSubscription<T> extends Link<ScalarSubscriptionQueue.ScalarSubscription<T>> {
        private final ScalarSubscriptionQueue<T> parent;
        private ScalarSubscriber subscriber;
        private boolean initialized = false; // todo get rid of it

        public ScalarSubscription(ScalarSubscriptionQueue<T> parent, ScalarSubscriber subscriber) {
            this.subscriber = subscriber;
            this.parent = parent;
        }

        public synchronized boolean isCancelled() {
            return subscriber == null;
        }

        public synchronized void setInitialized() {
            initialized = true;
            notifyAll();
        }

        public synchronized void waitInitialized() {
            while (!initialized) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    if (subscriber!=null) {
                        subscriber.onError(e);
                    }
                }
            }
        }

        public synchronized void cancel() {
            synchronized(this) {
                if (isCancelled()) {
                    return;
                }
                subscriber = null;
            }
            parent.cancel(this);
        }

        protected synchronized ScalarSubscriber extractScalarSubscriber() {
            waitInitialized();
            if (isCancelled()) {
                return null;
            } else {
                ScalarSubscriber subscriberLoc = subscriber;
                subscriber = null;
                return subscriberLoc;
            }
        }

        public <T> void onComplete(T value) {
            ScalarSubscriber subscriberLoc;
            synchronized (this) {
                if (isCancelled()) {
                    return;
                }
                subscriberLoc = subscriber;
            }
            subscriberLoc.onComplete(value);
        }

        public void onError(Throwable t) {
            ScalarSubscriber subscriberLoc = extractScalarSubscriber();
            if (subscriberLoc == null) {
                return;
            }
            subscriberLoc.onError(t);
        }
    }
}
