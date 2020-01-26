package org.df4j.core.util.linked;

import java.util.AbstractQueue;
import java.util.Iterator;

public class LinkedQueue<T> extends AbstractQueue<Link<T>> {
    private Link<T> header = new LinkImpl<T>();
    private int size = 0;

    @Override
    public Iterator<Link<T>> iterator() {
        return new LinkIterator();
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public boolean offer(Link<T> item) {
        header.offer(item);
        size++;
        return true;
    }

    @Override
    public Link<T> poll() {
        if (size == 0) {
            return null;
        }
        size--;
        return header.poll();
    }

    @Override
    public Link<T> peek() {
        Link<T> next = header.getNext();
        if (next == header) {
            return null;
        } else {
            return next;
        }
    }

    public boolean remove(Link<T> subscription) {
        if (subscription.isLinked()) {
            subscription.unlink();
            size--;
            return true;
        } else {
            return false;
        }
    }

    private class LinkIterator implements Iterator<Link<T>> {
        Link<T> current = header;
        boolean hasnext;

        @Override
        public boolean hasNext() {
            hasnext = current.getNext() != header;
            return hasnext;
        }

        @Override
        public Link<T> next() {
            if (!hasnext) {
                throw new IllegalStateException();
            }
            hasnext = false;
            current = current.getNext();
            return current;
        }

        @Override
        public void remove() {
            Link<T> res = current;
            current = res.getNext();
            res.unlink();
            hasnext = false;
        }
    }
}
