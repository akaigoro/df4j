package org.df4j.core.asyncproc;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.AbstractQueue;
import java.util.Iterator;

public class SubscriptionQueue<T> extends AbstractQueue<SubscriptionQueue.ScalaSubscription> {
    private ScalaSubscription first = null;
    private ScalaSubscription last = null;
    private int size = 0;

    public <T> void subscribe(Subscriber<? super T> s) {
        add(new ScalaSubscription(s));
    }

    @Override
    public Iterator<SubscriptionQueue.ScalaSubscription> iterator() {
        return null;
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public boolean offer(SubscriptionQueue.ScalaSubscription newSubscription) {
        newSubscription.prev = null;
        if (last == null) {
            last = first = newSubscription;
        } else {
            last = last.prev = newSubscription;
        }
        return true;
    }

    @Override
    public synchronized ScalaSubscription poll() {
        if (first == null) {
            return null;
        }
        ScalaSubscription res = first;
        if (first == last) {
            last = first = null;
        } else {
            first = res.prev;
        }
        return res;
    }

    @Override
    public ScalaSubscription peek() {
        return null;
    }

    synchronized boolean remove(ScalaSubscription subscription) {
        if (first == subscription) {
            poll();
            return true;
        }
        ScalaSubscription next = first;
        ScalaSubscription current = next.prev;
        while (current != null) {
            if (current == subscription) {
                next.prev = current.prev;
                if (current == last) {
                    last = next;
                }
                return true;
            }
        }
        return false;
    }

    void serveRequest(ScalaSubscription simpleSubscription) {
        throw new UnsupportedOperationException();
    }

    protected class ScalaSubscription implements Subscription {
        Subscriber subscriber;
        long requested = 0;

        ScalaSubscription prev;

        ScalaSubscription(Subscriber subscriber) {
            this.subscriber = subscriber;
        }

        synchronized boolean isCancelled() {
            return subscriber == null;
        }

        @Override
        public synchronized void request(long n) {
            if (n <= 0) {
                throw new IllegalArgumentException();
            }
            if (isCancelled()) {
                return;
            }
            boolean wasPassive = requested == 0;
            requested += n;
            if (requested <0) { // overflow
                requested = Long.MAX_VALUE;
            }
            if (wasPassive) {
                serveRequest(this);
            }
        }

        @Override
        public synchronized void cancel() {
            if (isCancelled()) {
                return;
            }
            remove(this);
            subscriber = null;
        }

        public void onNext(T value) {
            Subscriber subscriberLoc;
            synchronized(this) {
                if (isCancelled()) {
                    return;
                }
                subscriberLoc = subscriber;
                subscriber = null;
            }
            subscriberLoc.onNext(value);
        }

        public void onError(Throwable t) {
            Subscriber subscriberLoc;
            synchronized(this) {
                if (isCancelled()) {
                    return;
                }
                subscriberLoc = subscriber;
                subscriber = null;
            }
            subscriberLoc.onError(t);
        }
    }
}
