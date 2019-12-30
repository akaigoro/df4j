package org.df4j.core.port;

import org.df4j.core.dataflow.BasicBlock;
import org.df4j.protocol.Flood;
import org.df4j.protocol.Scalar;

import java.util.LinkedList;
import java.util.Queue;

/**
 * Token storage with standard Subscriber&lt;T&gt; interface.
 * It contains LinkedList for unlimited set of input tokens.
 * Backpressure not supported, so OOME is possible.
 *
 * @param <T> type of accepted tokens.
 */
public class InpFlood<T> extends BasicBlock.Port implements Flood.Subscriber<T>, InpMessagePort<T> {
    /** TODO optimize for single token */
    private  final Queue<T> tokens = new LinkedList<T>();
    private Throwable completionException;
    protected volatile boolean completed;
    Scalar.Subscription subscription;

    /**
     * @param parent {@link BasicBlock} to which this port belongs
     */
    public InpFlood(BasicBlock parent) {
        parent.super(false);
    }

    public boolean isCompleted() {
        plock.lock();
        try {
            return completed && tokens.isEmpty();
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
            return tokens.peek();
        } finally {
            plock.unlock();
        }
    }

    public  T poll() {
        plock.lock();
        try {
            if (!isReady()) {
                return null;
            }
            T res = tokens.poll();
            if (tokens.isEmpty()) {
                block();
            }
            return res;
        } finally {
            plock.unlock();
        }
    }

    public T remove() {
        plock.lock();
        try {
            T res = tokens.remove();
            if (tokens.isEmpty()) {
                block();
            }
            return res;
        } finally {
            plock.unlock();
        }
    }

    public boolean remove(T token) {
        plock.lock();
        try {
            boolean res = tokens.remove(token);
            if (tokens.isEmpty()) {
                block();
            }
            return res;
        } finally {
            plock.unlock();
        }
    }

    @Override
    public void onSubscribe(Scalar.Subscription subscription) {
        this.subscription = subscription;
    }

    @Override
    public void onNext(T message) {
        plock.lock();
        try {
            if (message == null) {
                throw new IllegalArgumentException();
            }
            if (completed) {
                return;
            }
            tokens.add(message);
            unblock();
        } finally {
            plock.unlock();
        }
    }

    @Override
    public void onError(Throwable throwable) {
        plock.lock();
        try {
            if (completed) {
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

    public void reset() {
        plock.lock();
        try {
            if (subscription != null) {
                subscription.cancel();
            }
            subscription = null;
            completionException = null;
            completed = false;
            for (;;) {
                T t = tokens.poll();
                if (t == null) {
                    break;
                }
            }
            block();
        } finally {
            plock.unlock();
        }
    }
}
