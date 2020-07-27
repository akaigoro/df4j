package org.df4j.core.port;

import org.df4j.core.dataflow.AsyncProc;
import org.df4j.protocol.Flood;
import org.df4j.protocol.SimpleSubscription;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.CompletionException;

/**
 * Token storage with standard Subscriber&lt;T&gt; interface.
 * It contains LinkedList for unlimited set of input tokens.
 * Backpressure not supported, so OOME is possible.
 *
 * @param <T> type of accepted tokens.
 */
public class InpFlood<T> extends CompletablePort implements InpMessagePort<T> , Flood.Subscriber<T> {
    /** TODO optimize for single token */
    private  final Queue<T> tokens = new LinkedList<>();
    protected SimpleSubscription subscription;

    /**
     * @param parent {@link AsyncProc} to which this port belongs
     */
    public InpFlood(AsyncProc parent) {
        super(parent);
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

    public  T poll() {
        synchronized(parent) {
            if (!isReady()) {
                return null;
            }
            T res = tokens.poll();
            if (tokens.isEmpty()) {
                block();
            }
            return res;
        }
    }

    @Override
    public T remove() throws CompletionException {
        if (isCompleted()) {
            throw new java.util.concurrent.CompletionException(completionException);
        }
        T res = poll();
        return res;
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
        synchronized(parent) {
            if (message == null) {
                throw new IllegalArgumentException();
            }
            if (completed) {
                return;
            }
            tokens.add(message);
            unblock();
        }
    }
}
