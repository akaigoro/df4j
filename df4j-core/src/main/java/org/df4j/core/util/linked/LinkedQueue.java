package org.df4j.core.util.linked;

import java.util.AbstractQueue;
import java.util.Iterator;

public class LinkedQueue<T extends Link> extends AbstractQueue<T> {
    private Link<T> header = new HeaderLink();
    private int size = 0;

    @Override
    public Iterator<T> iterator() {
        return new LinkIterator();
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public boolean offer(T item) {
        if (item == header) {
            throw new IllegalArgumentException();
        }
        item.setNext(header);
        Link<T> prev = header.getPrev();
        item.setPrev(prev);
        prev.setNext(item);
        header.setPrev(item);
        size++;
        return true;
    }

    @Override
    public T poll() {
        if (size == 0) {
            return null;
        }
        size--;
        Link<T> first = null;
        Link<T> res = header.getNext();
        if (res != this) {
            res.unlink();
            first = res;
        }
        if (first == null) {
            return  null;
        }else {
            return first.getItem();
        }
    }

    @Override
    public T peek() {
        Link<T> next = header.getNext();
        if (next == header) {
            return null;
        } else {
            return next.getItem();
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

    private class LinkIterator implements Iterator<T> {
        Link<T> current = header;
        boolean hasnext;

        @Override
        public boolean hasNext() {
            hasnext = current.getNext() != header;
            return hasnext;
        }

        @Override
        public T next() {
            if (!hasnext) {
                throw new IllegalStateException();
            }
            hasnext = false;
            current = current.getNext();
            return current.getItem();
        }

        @Override
        public void remove() {
            Link<T> res = current;
            current = res.getNext();
            res.unlink();
            hasnext = false;
        }
    }

    private class HeaderLink extends LinkImpl<T> {
        @Override
        public T getItem() {
            return null;
        }
    }
}
