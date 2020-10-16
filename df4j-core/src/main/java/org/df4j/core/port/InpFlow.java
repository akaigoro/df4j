package org.df4j.core.port;

import org.df4j.core.dataflow.AsyncProc;
import org.df4j.core.dataflow.Transitionable;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.nio.BufferOverflowException;
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
    public InpFlow(Transitionable parent, int capacity) {
        super(parent);
        setCapacity(capacity);
    }

    /**
     * @param parent {@link AsyncProc} to which this port belongs
     */
    public InpFlow(Transitionable parent) {
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
        tokens = new ArrayDeque<>(capacity);
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
        synchronized(transition1) {
            return completed && tokens.isEmpty();
        }
    }

    public T current() {
        synchronized(transition1) {
            return tokens.peek();
        }
    }

    @Override
    public void onSubscribe(Subscription subscription) {
        synchronized(transition1) {
            if (this.subscription != null) {
                subscription.cancel(); // this is dictated by the spec.
                return;
            }
            this.subscription = subscription;
            if (tokens.isEmpty()) {
                block();
            }
            requestedCount = remainingCapacity();
            if (requestedCount == 0) {
                return;
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
        synchronized(transition1) {
            if (message == null) {
                throw new NullPointerException();
            }
            if (completed) {
                return;
            }
            if (subscription != null) {
                requestedCount--;
            }
            if (tokens.size() == bufferCapacity) {
                throw new BufferOverflowException();
            }
            tokens.add(message);
            unblock();
        }
    }

    public T poll() {
        long n;
        T res;
        synchronized(transition1) {
            if (!ready) {
                throw new IllegalStateException();
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
    public T remove() throws CompletionException {
        T res = poll();
        if (res == null) {
            throw new java.util.concurrent.CompletionException(completionException);
        }
        return res;
    }

    public void cancel() {
        Subscription sub;
        synchronized (transition1) {
            sub = subscription;
            onComplete();
            if (sub == null) {
                return;
            }
            subscription = null;
        }
        sub.cancel();
    }
}
