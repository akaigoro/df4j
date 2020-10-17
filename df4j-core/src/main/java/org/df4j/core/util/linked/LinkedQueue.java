package org.df4j.core.util.linked;

import java.util.AbstractQueue;
import java.util.Collection;
import java.util.Iterator;

public class LinkedQueue<L extends Link> {
    private LinkImpl header = new LinkImpl();

    public Iterator<L> iterator() {
        return new LinkIterator();
    }

    public boolean isEmpty() {
        return !header.isLinked();
    }

    public synchronized boolean offer(L item) {
        if (item.isLinked()) {
            throw new IllegalArgumentException();
        }
        item.setNext(header);
        Link prev = header.getPrev();
        item.setPrev(prev);
        prev.setNext(item);
        header.setPrev(item);
        return true;
    }

    /**
     * Inserts the specified element into this queue if it is possible to do so
     * immediately without violating capacity restrictions, returning
     * {@code true} upon success and throwing an {@code IllegalStateException}
     * if no space is currently available.
     *
     * <p>This implementation returns {@code true} if {@code offer} succeeds,
     * else throws an {@code IllegalStateException}.
     *
     * @param e the element to add
     * @return {@code true} (as specified by {@link Collection#add})
     * @throws IllegalStateException if the element cannot be added at this
     *         time due to capacity restrictions
     * @throws ClassCastException if the class of the specified element
     *         prevents it from being added to this queue
     * @throws NullPointerException if the specified element is null and
     *         this queue does not permit null elements
     * @throws IllegalArgumentException if some property of this element
     *         prevents it from being added to this queue
     */
    public boolean add(L e) {
        if (offer(e)) {
            return true;
        } else {
            throw new IllegalStateException("Queue full");
        }
    }

    public L poll() {
        if (isEmpty()) {
            return null;
        }
        Link first = null;
        Link res = header.getNext();
        if (res != this) {
            res.unlink();
            first = res;
        }
        if (first == null) {
            return  null;
        } else {
            return (L)first;
        }
    }

    public L peek() {
        Link next = header.getNext();
        if (next == header) {
            return null;
        } else {
            return (L)next;
        }
    }

    public boolean remove(Link item) {
        if (item.isLinked()) {
            item.unlink();
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

    /**
     * Returns a string representation of this collection.  The string
     * representation consists of a list of the collection's elements in the
     * order they are returned by its iterator, enclosed in square brackets
     * ({@code "[]"}).  Adjacent elements are separated by the characters
     * {@code ", "} (comma and space).  Elements are converted to strings as
     * by {@link String#valueOf(Object)}.
     *
     * @return a string representation of this collection
     */
    public String toString() {
        Iterator<L> it = iterator();
        if (! it.hasNext())
            return "[]";

        StringBuilder sb = new StringBuilder();
        sb.append('[');
        for (;;) {
            L e = it.next();
            sb.append(e == this ? "(this Collection)" : e);
            if (! it.hasNext())
                return sb.append(']').toString();
            sb.append(',').append(' ');
        }
    }
}
