package org.df4j.core.port;

import org.df4j.core.dataflow.AsyncProc;
import org.reactivestreams.*;

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
    /** extracted token */
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
        tokens = new ArrayDeque<T>(capacity - 1);
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
        plock.lock();
        try {
            return completed && tokens.isEmpty();
        } finally {
            plock.unlock();
        }
    }

    public T current() {
        plock.lock();
        try {
            return tokens.peek();
        } finally {
            plock.unlock();
        }
    }

    @Override
    public void onSubscribe(Subscription subscription) {
        plock.lock();
        try {
            if (this.subscription != null) {
                subscription.cancel(); // this is dictated by the spec.
                return;
            }
            this.subscription = subscription;
            requestedCount = remainingCapacity();
            if (tokens.isEmpty()) {
                block();
            }
        } finally {
            plock.unlock();
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
        plock.lock();
        try {
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
        } finally {
            plock.unlock();
        }
    }

    public T remove() {
        plock.lock();
        long n;
        T res;
        try {
            if (!ready) {
                throw new IllegalStateException();
            }
            if (completed) {
                throw new CompletionException(completionException);
            }
            res = tokens.remove();
            if (tokens.isEmpty()) {
                block();
            }
            if (subscription == null) {
                return res;
            }
            n = remainingCapacity();
            requestedCount += n;
        } finally {
            plock.unlock();
        }
        subscription.request(n);
        return res;
    }
}
