package org.df4j.core.port;

import org.df4j.core.dataflow.AsyncProc;
import org.df4j.protocol.Flood;
import org.df4j.protocol.SimpleSubscription;

import java.util.LinkedList;
import java.util.Queue;

/**
 * Token storage with standard Subscriber&lt;T&gt; interface.
 * It contains LinkedList for unlimited set of input tokens.
 * Backpressure not supported, so OOME is possible.
 *
 * @param <T> type of accepted tokens.
 */
public class InpFlood<T> extends CompletablePort implements Flood.Subscriber<T>, InpMessagePort<T> {
    /** TODO optimize for single token */
    private  final Queue<T> tokens = new LinkedList<T>();
    protected SimpleSubscription subscription;

    /**
     * @param parent {@link AsyncProc} to which this port belongs
     * @param active initial port state
     */
    public InpFlood(AsyncProc parent, boolean active) {
        super(parent, false, active);
    }

    public InpFlood(AsyncProc parent) {
        super(parent);
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
    public void onSubscribe(SimpleSubscription subscription) {
        this.subscription = subscription;
        if (tokens.isEmpty()) {
            block();
        }
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
}
