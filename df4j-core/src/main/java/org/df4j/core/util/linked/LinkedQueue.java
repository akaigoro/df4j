package org.df4j.core.util.linked;

import java.util.AbstractQueue;
import java.util.Iterator;

public abstract class LinkedQueue<L extends Link<L>> extends AbstractQueue<L> {
    private L first = null;
    private L last = null;
    private volatile int size = 0;

    private L remove(L next, L current) {
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
    public Iterator<L> iterator() {
        Iterator subscriptionIterator = new SubscriptionIterator();
        return subscriptionIterator;
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public synchronized boolean offer(L subscription) {
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
        L next = null;
        L current = first;
        while (current != null) {
            if (subscription == current) {
                next = remove(next, current);
                return;
            }
        }
    }

    private class SubscriptionIterator implements Iterator<Link<L>> {
        L next;
        L current = null;
        L prev;

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
