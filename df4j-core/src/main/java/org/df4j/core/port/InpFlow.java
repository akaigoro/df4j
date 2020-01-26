package org.df4j.core.port;

import org.df4j.core.dataflow.BasicBlock;
import org.reactivestreams.*;

import java.util.ArrayDeque;

/**
 * Token storage with standard Subscriber&lt;T&gt; interface.
 *
 * @param <T> type of accepted tokens.
 */
public class InpFlow<T> extends BasicBlock.Port implements Subscriber<T>, InpMessagePort<T> {
    private int bufferCapacity;
    private boolean lazy = false;
    protected boolean withBuffer;
    private ArrayDeque<T> buff;
    /** extracted token */
    protected T value;
    private Throwable completionException;
    protected volatile boolean completed;
    protected Subscription subscription;
    private long requestedCapacity;

    /**
     * creates a port which is subscribed to the {@code #publisher}
     * @param parent {@link BasicBlock} to wich this port belongs
     * @param capacity required capacity
     */
    public InpFlow(BasicBlock parent, int capacity) {
        parent.super(false);
        setCapacity(capacity);
    }

    /**
     * @param parent {@link BasicBlock} to which this port belongs
     */
    public InpFlow(BasicBlock parent) {
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
        int cap1 = value == null ? 1 : 0;
        int cap2 = withBuffer? bufferCapacity-buff.size() : 0;
        return cap1 + cap2 - requestedCapacity;
    }

    private int getBufferCapacity() {
        return !withBuffer? 0 : this.bufferCapacity;
    }

    public boolean isLazy() {
        return lazy;
    }

    public void setLazy(boolean lazy) {
        this.lazy = lazy;
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
        this.subscription = subscription;
        if (lazy) {
            return;
        }
        long remainingCapacity;
        plock.lock();
        try {
            remainingCapacity = remainingCapacity();
            requestedCapacity = remainingCapacity;
        } finally {
            plock.unlock();
        }
        subscription.request(remainingCapacity);
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
            requestedCapacity += n;
        } finally {
            plock.unlock();
        }
        subscription.request(n);
    }

    @Override
    public void onNext(T message) {
        plock.lock();
        try {
            if (message == null) {
                throw new IllegalArgumentException();
            }
            if (isCompleted()) {
                return;
            }
            requestedCapacity--;
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
            if (subscription == null) {
                return res;
            }
            return res;
        } finally {
            plock.unlock();
        }
    }

    public T removeAndRequest() {
        plock.lock();
        try {
            if (!ready) {
                throw new IllegalStateException();
            }
            T res = value;
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
            long n = remainingCapacity();
            requestedCapacity += n;
            subscription.request(n);
            return res;
        } finally {
            plock.unlock();
        }
    }

    @Override
    public void onError(Throwable throwable) {
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
    public void onComplete() {
        onError(null);
    }

    protected void roomExhausted(){}
    protected void roomAvailable(){}
}
