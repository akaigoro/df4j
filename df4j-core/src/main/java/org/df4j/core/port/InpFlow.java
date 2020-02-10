package org.df4j.core.port;

import org.df4j.core.dataflow.AsyncProc;
import org.reactivestreams.*;

import java.util.ArrayDeque;

/**
 * Token storage with standard Subscriber&lt;T&gt; interface.
 *
 * @param <T> type of accepted tokens.
 */
public class InpFlow<T> extends AsyncProc.Port implements InpMessagePort<T>, Subscriber<T> {
    private int bufferCapacity;
    protected boolean withBuffer;
    private ArrayDeque<T> buff;
    /** extracted token */
    protected T value;
    private Throwable completionException;
    protected volatile boolean completed;
    protected Subscription subscription;
    private long requestedCount;

    /**
     * creates a port which is subscribed to the {@code #Flow.Publisher}
     * @param parent {@link AsyncProc} to wich this port belongs
     * @param capacity required capacity
     */
    public InpFlow(AsyncProc parent, int capacity) {
        parent.super(false);
        setCapacity(capacity);
    }

    public InpFlow(AsyncProc parent, int capacity, boolean active) {
        parent.super(false, active);
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
        if (capacity == getBufferCapacity()) {
            return;
        }
        bufferCapacity = capacity;
        withBuffer = capacity > 1;
        if (withBuffer) {
            buff = new ArrayDeque<T>(capacity - 1);
        } else {
            buff = null;
        }
    }

    private boolean buffIsFull() {
        return !withBuffer || buff.size() == bufferCapacity;
    }

    private long remainingCapacity() {
        if (requestedCount < 0) {
            throw new IllegalStateException();
        }
        int cap1 = value == null ? 1 : 0;
        int cap2 = withBuffer? bufferCapacity-buff.size() : 0;
        long res = cap1 + cap2 - requestedCount;
        if (res < 0) {
            throw new IllegalStateException();
        }
        return res;
    }

    private int getBufferCapacity() {
        return !withBuffer? 0 : this.bufferCapacity;
    }

    public boolean isCompleted() {
        plock.lock();
        try {
            return completed && value==null;
        } finally {
            plock.unlock();
        }
    }

    public Throwable getCompletionException() {
        return completionException;
    }

    public boolean isCompletedExceptionslly() {
        return completionException != null;
    }

    public T current() {
        plock.lock();
        try {
            return value;
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
            if (value == null) {
                block();
            }
        } finally {
            plock.unlock();
        }
        subscription.request(requestedCount);
    }

    public void request(long n) {
        if (n <= 0) {
            throw new IllegalArgumentException();
        }
        plock.lock();
        try {
            if (n > remainingCapacity()) {
                throw new IllegalArgumentException();
            }
            requestedCount += n;
        } finally {
            plock.unlock();
        }
        subscription.request(n);
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
            if (value == null) {
                value = message;
                unblock();
            } else if (buffIsFull()) {
                throw new IllegalStateException("buffer overflow");
            } else {
                buff.add(message);
            }
            if (buffIsFull()) {
                roomExhausted();
            }
        } finally {
            plock.unlock();
        }
    }

    public T remove() {
        plock.lock();
        try {
            if (!isReady()) {
                throw new IllegalStateException();
            }
            T res = value;
            value = null;
            if (!withBuffer || buff.isEmpty()) {
                block();
            } else {
                value = buff.poll();
            }
            roomAvailable();
            return res;
        } finally {
            plock.unlock();
        }
    }

    public T removeAndRequest() {
        plock.lock();
        long n;
        T res;
        try {
            if (!ready) {
                throw new IllegalStateException();
            }
            res = value;
            value = null;
            if (withBuffer && !buff.isEmpty()) {
                value = buff.poll();
            } else if (!completed) {
                block();
            }
            roomAvailable();
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

    private void onComplete(Throwable throwable) {
        plock.lock();
        try {
            if (isCompleted()) {
                return;
            }
            this.completed = true;
            this.completionException = throwable;
            subscription = null;
            unblock();
        } finally {
            plock.unlock();
        }
    }

    @Override
    public void onError(Throwable throwable) {
        if (throwable == null) {
            throw new NullPointerException();
        }
        onComplete(throwable);
    }

    @Override
    public void onComplete() {
        onComplete(null);
    }

    protected void roomExhausted(){}
    protected void roomAvailable(){}
}
