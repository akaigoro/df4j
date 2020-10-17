package org.df4j.core.port;

import org.df4j.core.dataflow.AsyncProc;
import org.jetbrains.annotations.Nullable;
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
        synchronized(parent) {
            return completed && tokens.isEmpty();
        }
    }

    public T current() {
        synchronized(parent) {
            return tokens.peek();
        }
    }

    protected void addToken(T message) {
        tokens.add(message);
        unblock();
    }

    @Override
    protected void _onComplete(Throwable throwable) {
        super._onComplete(throwable);
        if (tokens.isEmpty()) {
            whenComplete();
        }
    }

    @Nullable
    private T pollToken() {
        T res;
        res = tokens.poll();
        if (tokens.isEmpty()) {
            if (completed) {
                whenComplete();
            } else {
                block();
            }
        }
        return res;
    }

    protected void whenComplete() {
    }

    @Override
    public void onSubscribe(Subscription subscription) {
        synchronized(this) {
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

    public synchronized boolean offer(T message) {
        if (message == null) {
            throw new NullPointerException();
        }
        if (isCompleted()) {
            return false;
        }
        if (buffIsFull()) {
            return false;
        }
        tokens.addLast(message);
        unblock();
        return true;
    }

    /**
     * normally this method is called by Flow.Publisher.
     * But before the port is subscribed, this method can be called directly.
     * @throws IllegalArgumentException when argument is null
     * @throws IllegalStateException if no room left to store argument
     * @param message token to store
     */
    @Override
    public synchronized void onNext(T message) {
        if (message == null) {
            throw new NullPointerException();
        }
        if (isCompleted()) {
            return;
        }
        if (buffIsFull()) {
            throw new IllegalStateException();
        }
        if (subscription != null) {
            requestedCount--;
        }
        addToken(message);
    }

    public synchronized void put(T message) throws InterruptedException {
        if (message == null) {
            throw new NullPointerException();
        }
        if (isCompleted()) {
            return;
        }
        while (buffIsFull()) {
            wait();
        }
        addToken(message);
    }

    public T poll() {
        long n;
        T res;
        synchronized(parent) {
            if (!ready) {
                throw new IllegalStateException();
            }
            res = pollToken();
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
        if (isCompleted()) {
            throw new java.util.concurrent.CompletionException(completionException);
        }
        T res = poll();
        if (res == null) {
            throw new IllegalStateException();
        }
        return res;
    }

    public int size() {
        return tokens.size();
    }

    public boolean isEmpty() {
        return tokens.isEmpty();
    }

    static final AsyncProc.AbstractPort dummyListener = new AsyncProc.AbstractPort() {

        @Override
        public boolean isReady() {
            return false;
        }

        @Override
        public void block() {}

        @Override
        public void unblock() {}
    };
}
