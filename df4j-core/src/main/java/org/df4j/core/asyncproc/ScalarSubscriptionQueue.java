package org.df4j.core.asyncproc;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

import java.util.AbstractQueue;
import java.util.Iterator;

public class ScalarSubscriptionQueue<T> extends AbstractQueue<ScalarSubscription<T>>
    implements Publisher<T>, SubscriptionListener<T>
{
    private ScalarSubscription first = null;
    private ScalarSubscription last = null;
    private volatile int size = 0;

    @Override
    public void subscribe(Subscriber<? super T> s) {
        add(new ScalarSubscription(this, s));
    }

    @Override
    public Iterator<ScalarSubscription<T>> iterator() {
        return new Iterator<ScalarSubscription<T>>(){
            ScalarSubscription<T> prev;
            ScalarSubscription<T> current;
            ScalarSubscription<T> next;

            @Override
            public boolean hasNext() {
                next = current==null?first:current.prev;
                return next != null;
            }

            @Override
            public ScalarSubscription<T> next() {
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
                    first = current.prev;
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
    public synchronized boolean offer(ScalarSubscription<T> newSubscription) {
        if (newSubscription.prev != null) {
            throw new IllegalStateException();
        }
        if (last == null) {
            last = first = newSubscription;
        } else {
            last = last.prev = newSubscription;
        }
        size++;
        return true;
    }

    @Override
    public synchronized ScalarSubscription<T> poll() {
        if (first == null) {
            return null;
        }
        ScalarSubscription<T> res = first;
        if (first == last) {
            last = first = null;
        } else {
            first = res.prev;
        }
        res.prev = null;
        size--;
        return res;
    }

    @Override
    public ScalarSubscription<T> peek() {
        return first;
    }

    public synchronized boolean remove(ScalarSubscription<T> subscription) {
        for  (Iterator<ScalarSubscription<T>> it = iterator(); it.hasNext();) {
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
