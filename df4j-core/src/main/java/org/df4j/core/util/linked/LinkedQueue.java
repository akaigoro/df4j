package org.df4j.core.util.linked;

import java.util.AbstractQueue;
import java.util.Iterator;

public class LinkedQueue<L extends Link> extends AbstractQueue<L> {
    private LinkImpl header = new LinkImpl();
    private int size = 0;

    @Override
    public Iterator<L> iterator() {
        return new LinkIterator();
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public boolean offer(L item) {
        if (item == header) {
            throw new IllegalArgumentException();
        }
        item.setNext(header);
        Link prev = header.getPrev();
        item.setPrev(prev);
        prev.setNext(item);
        header.setPrev(item);
        size++;
        return true;
    }

    @Override
    public L poll() {
        if (size == 0) {
            return null;
        }
        size--;
        Link first = null;
        Link res = header.getNext();
        if (res != this) {
            res.unlink();
            first = res;
        }
        if (first == null) {
            return  null;
        }else {
            return (L)first;
        }
    }

    @Override
    public L peek() {
        Link next = header.getNext();
        if (next == header) {
            return null;
        } else {
            return (L)next;
        }
    }

    public boolean remove(Link subscription) {
        if (subscription.isLinked()) {
            subscription.unlink();
            size--;
            return true;
        } else {
            return false;
        }
    }

    private class LinkIterator implements Iterator<L> {
        Link current = header;
        boolean hasnext;

        @Override
        public boolean hasNext() {
            hasnext = current.getNext() != header;
            return hasnext;
        }

        @Override
        public L next() {
            if (!hasnext) {
                throw new IllegalStateException();
            }
            hasnext = false;
            current = current.getNext();
            return (L)current;
        }

        @Override
        public void remove() {
            Link res = current;
            current = res.getNext();
            res.unlink();
            hasnext = false;
        }
    }
}
