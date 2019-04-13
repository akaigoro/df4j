package org.df4j.core.util.linked;

import java.util.AbstractQueue;
import java.util.Iterator;

public abstract class LinkedQueue<L extends Link<L>> extends AbstractQueue<Link<L>> {
    private Link<L> first = null;
    private Link<L> last = null;
    private volatile int size = 0;

    private Link<L> remove(Link<L> next, Link<L> current) {
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
    public Iterator<Link<L>> iterator() {
        Iterator subscriptionIterator = new SubscriptionIterator();
        return subscriptionIterator;
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public synchronized boolean offer(Link<L> subscription) {
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
    public synchronized L poll() {
        if (first == null) {
            return null;
        }
        Link<L> res = first;
        if (first == last) {
            last = first = null;
        } else {
            first = res.prev;
        }
        res.prev = null;
        size--;
        return (L) res;
    }

    @Override
    public L peek() {
        return (L) first;
    }

    public synchronized void cancel(L subscription) {
        Link<L> next = null;
        Link<L> current = first;
        while (current != null) {
            if (subscription == current) {
                next = remove(next, current);
                return;
            }
        }
    }

    public void activate(L simpleSubscription) {
        throw new UnsupportedOperationException();
    }

    private class SubscriptionIterator implements Iterator<Link<L>> {
        Link<L> next;
        Link<L> current = null;
        Link<L> prev;

        @Override
        public boolean hasNext() {
            next = current == null ? first : current.prev;
            return next != null;
        }

        @Override
        public Link<L> next() {
            prev = current;
            current = next;
            return current;
        }

        @Override
        public void remove() {
            LinkedQueue.this.remove(next, current);
        }
    }
}
