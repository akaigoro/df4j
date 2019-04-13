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
/*
    @Override
    public abstract void subscribe(Subscriber<? super T> s);
*/
    @Override
    public Iterator<S> iterator() {
        return new Iterator<S>(){
            S prev;
            S current = null;
            S next;

            @Override
            public boolean hasNext() {
                next = current == null? first: current.prev;
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
                throw new UnsupportedOperationException();
            }
        };
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

    /**
     * For simplicity, cancelled subscription is actually removed only
     * when it reaches the first position.
     */
    private void clearCancelled() {
        S newFirst = first;
        if (newFirst == null) {
            return;
        }
        if (!newFirst.isCancelled()) {
            return;
        }
        for (;;) {
            newFirst.prev = null;
            newFirst = newFirst.prev;
            if (newFirst == null) {
                break;
            }
            if (!newFirst.isCancelled()) {
                break;
            }
        }
        first = newFirst;
        if (newFirst == null) {
            last = null;
        }
    }


    @Override
    public synchronized S poll() {
        clearCancelled();
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
        size--;
        if (subscription == first) {
            clearCancelled();
        }
    }

    public void activate(S simpleSubscription) {
        throw new UnsupportedOperationException();
    }

}
