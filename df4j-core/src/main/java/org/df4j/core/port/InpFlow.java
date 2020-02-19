package org.df4j.core.port;

import org.df4j.core.dataflow.AsyncProc;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.ArrayDeque;
import java.util.concurrent.CompletionException;

/**
 * Token storage with standard Subscriber&lt;T&gt; interface.
 *
 * @param <T> type of accepted tokens.
 */
public class InpFlow<T> extends CompletablePort implements InpMessagePort<T>, Subscriber<T> {
    private int bufferCapacity;
    private ArrayDeque<T> tokens;
    protected Subscription subscription;
    private long requestedCount;

    /**
     * creates a port which is subscribed to the {@code #Flow.Publisher}
     * @param parent {@link AsyncProc} to wich this port belongs
     * @param capacity required capacity
     */
    public InpFlow(AsyncProc parent, int capacity) {
        super(parent);
        setCapacity(capacity);
    }

    public InpFlow(AsyncProc parent, int capacity, boolean active) {
        super(parent, false, active);
        setCapacity(capacity);
    }

    /**
     * @param parent {@link AsyncProc} to which this port belongs
     */
    public InpFlow(AsyncProc parent) {
        this(parent, 1);
    }

    public void setCapacity(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException();
        }
        if (capacity == this.bufferCapacity) {
            return;
        }
        bufferCapacity = capacity;
        tokens = new ArrayDeque<T>(capacity);
    }

    private boolean buffIsFull() {
        return tokens.size() == bufferCapacity;
    }

    private long remainingCapacity() {
        if (requestedCount < 0) {
            throw new IllegalStateException();
        }
        long res = bufferCapacity- tokens.size() - requestedCount;
        if (res < 0) {
            throw new IllegalStateException();
        }
        return res;
    }

    public boolean isCompleted() {
        synchronized(parent) {
            return completed && tokens.isEmpty();
        }
    }

    public T current() {
        synchronized(parent) {
            return tokens.peek();
        }
    }

    @Override
    public void onSubscribe(Subscription subscription) {
        synchronized(parent) {
            if (this.subscription != null) {
                subscription.cancel(); // this is dictated by the spec.
                return;
            }
            this.subscription = subscription;
            requestedCount = remainingCapacity();
            if (tokens.isEmpty()) {
                block();
            }
        }
        subscription.request(requestedCount);
    }

    /**
     * normally this method is called by Flow.Publisher.
     * But before the port is subscribed, this method can be called directly.
     * @throws IllegalArgumentException when argument is null
     * @throws IllegalStateException if no room left to store argument
     * @param message token to store
     */
    @Override
    public void onNext(T message) {
        synchronized(parent) {
            if (message == null) {
                throw new NullPointerException();
            }
            if (isCompleted()) {
                return;
            }
            if (subscription != null) {
                requestedCount--;
            }
            tokens.add(message);
            unblock();
        }
    }

    public T poll() {
        long n;
        T res;
        synchronized(parent) {
            if (!ready) {
                throw new IllegalStateException();
            }
            if (isCompleted()) {
                throw new CompletionException(completionException);
            }
            res = tokens.poll();
            if (tokens.isEmpty() && !completed) {
                block();
            }
            if (subscription == null) {
                return res;
            }
            n = remainingCapacity();
            requestedCount += n;
        }
        subscription.request(n);
        return res;
    }

    @Override
    public T remove() {
        T res = poll();
        if (res == null) {
            throw new IllegalStateException();
        }
        return res;
    }
}
