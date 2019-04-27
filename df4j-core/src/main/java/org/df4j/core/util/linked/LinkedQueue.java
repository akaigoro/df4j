package org.df4j.core.util.linked;

import java.util.AbstractQueue;
import java.util.Iterator;

public class LinkedQueue<L extends Link<L>> extends AbstractQueue<L> {
    private L header = (L) new Link<L>();
    private int size = 0;

    @Override
    public Iterator<L> iterator() {
        Iterator subscriptionIterator = new LinkIterator();
        return subscriptionIterator;
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public boolean offer(L subscription) {
        if (subscription.isLinked()) {
            throw new IllegalStateException();
        }
        header.offer(subscription);
        size++;
        return true;
    }

    @Override
    public L poll() {
        if (size == 0) {
            return null;
        }
        size--;
        return header.poll();
    }

    @Override
    public L peek() {
        L next = header.getNext();
        if (next == header) {
            return null;
        } else {
            return next;
        }
    }

    public boolean remove(L subscription) {
        if (subscription.isLinked()) {
            subscription.unlink();
            return true;
        } else {
            return false;
        }
    }

    @Override
    public String toString() {
        return getClass().getName()+"(size="+size+")";
    }

    private class LinkIterator implements Iterator<Link<L>> {
        L current = header;
        boolean hasnext;

        @Override
        public boolean hasNext() {
            hasnext = current.next != header;
            return hasnext;
        }

        @Override
        public Link<L> next() {
            if (!hasnext) {
                throw new IllegalStateException();
            }
            hasnext = false;
            current = current.next;
            return current;
        }

        @Override
        public void remove() {
            L res = current;
            current = res.next;
            res.unlink();
            hasnext = false;
        }
    }
}
