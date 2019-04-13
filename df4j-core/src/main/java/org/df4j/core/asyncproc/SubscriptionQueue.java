package org.df4j.core.asyncproc;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

import java.util.AbstractQueue;
import java.util.Iterator;

public abstract class SubscriptionQueue<T, S extends LinkedSubscription<T,S>> extends AbstractQueue<S>
    implements SubscriptionListener<T, S>
{
    private S first = null;
    private S last = null;
    private volatile int size = 0;

    private S remove(S next, S current) {
        size--;
        if (next == null) {
            first = current.prev;
        } else {
            next = current.prev;
        }
        current.prev = null;
        if (current == last) {
            if (next == null) {
                first = last = null;
            } else {
                last = next;
            }
        }
        return next;
    }

    @Override
    public Iterator<S> iterator() {
        return new SubscriptionIterator();
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public synchronized boolean offer(S subscription) {
        if (subscription.isLinked()) {
            throw new IllegalStateException();
        }
        if (last == null) {
            last = first = subscription;
        } else {
            last.prev = subscription;
            last = subscription;
        }
        size++;
        return true;
    }

    @Override
    public synchronized S poll() {
        if (first == null) {
            return null;
        }
        S res = first;
        if (first == last) {
            last = first = null;
        } else {
            first = (S) res.prev;
        }
        res.prev = null;
        size--;
        return res;
    }

    @Override
    public S peek() {
        return first;
    }

    public synchronized void remove(S subscription) {
        S next = null;
        S current = first;
        while (current != null) {
            if (subscription == current) {
                next = remove(next, current);
                return;
            }
        }
    }

    public void activate(S simpleSubscription) {
        throw new UnsupportedOperationException();
    }

    private class SubscriptionIterator implements Iterator<S> {
        S next;
        S current = null;
        S prev;

        @Override
        public boolean hasNext() {
            next = current == null ? first : current.prev;
            return next != null;
        }

        @Override
        public S next() {
            prev = current;
            current = next;
            return current;
        }

        @Override
        public void remove() {
            SubscriptionQueue.this.remove(next, current);
        }
    }
}
