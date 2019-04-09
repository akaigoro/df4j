package org.df4j.core.asyncproc;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

import java.util.AbstractQueue;
import java.util.Iterator;

public abstract class SubscriptionQueue<T, S extends ScalarSubscription<T>> extends AbstractQueue<S>
    implements SubscriptionListener<T, S>, Publisher<T>
{
    private S first = null;
    private S last = null;
    private volatile int size = 0;

    @Override
    public abstract void subscribe(Subscriber<? super T> s);

    @Override
    public Iterator<S> iterator() {
        return new Iterator<S>(){
            S prev;
            S current;
            S next;

            @Override
            public boolean hasNext() {
                next = current==null?first: (S) current.prev;
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
                if (current != first) {
                    prev.prev = current.prev;
                    if (current == last) {
                        last = prev;
                    }
                } else if (first == last) {
                    first = last = null;
                } else {
                    first = (S) current.prev;
                }
                current.prev = null;
                size--;
            }
        };
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public synchronized boolean offer(S newSubscription) {
        if (newSubscription.prev != null) {
            throw new IllegalStateException();
        }
        if (last == null) {
            last = first = newSubscription;
        } else {
            last = (S) (last.prev = newSubscription);
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

    public synchronized boolean remove(S subscription) {
        for  (Iterator<S> it = iterator(); it.hasNext();) {
            if (it.next() == subscription) {
                it.remove();
                return true;
            }
        }
        return false;
    }

    public void serveRequest(ScalarSubscription simpleSubscription) {
        throw new UnsupportedOperationException();
    }

}
