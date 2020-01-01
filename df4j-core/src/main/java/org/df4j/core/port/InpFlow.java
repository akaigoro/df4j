package org.df4j.core.port;

import org.df4j.core.dataflow.BasicBlock;
import org.df4j.protocol.Flow;

import java.util.ArrayDeque;

/**
 * Token storage with standard Subscriber&lt;T&gt; interface.
 *
 * @param <T> type of accepted tokens.
 */
public class InpFlow<T> extends BasicBlock.Port implements Flow.Subscriber<T>, InpMessagePort<T> {
    private int bufferCapacity;
    protected boolean withBuffer;
    private ArrayDeque<T> buff;
    /** extracted token */
    protected T value;
    private Throwable completionException;
    protected volatile boolean completed;
    Flow.Subscription subscription;

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
        if (capacity == this.bufferCapacity) {
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
            subscription.request(1);
            return res;
        } finally {
            plock.unlock();
        }
    }

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        this.subscription = subscription;
        if (!isReady()) {
            subscription.request(1);
        }
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

    private boolean buffIsFull() {
        return !withBuffer || buff.size() == bufferCapacity;
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
