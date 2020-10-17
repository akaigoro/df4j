package org.df4j.core.port;

import org.df4j.core.connector.AsyncArrayBlockingQueue;
import org.df4j.core.dataflow.AsyncProc;
import org.df4j.protocol.Flow;
import org.reactivestreams.Subscriber;

/**
 * A passive source of messages (like a server).
 * Unblocked initially.
 * It has room for single message.
 * Blocked when overflow.
 * Is ready when has room to store at least one toke
 * @param <T> type of emitted tokens
 */
public class OutFlow<T> extends AsyncProc.Port
        implements OutMessagePort<T>, Flow.Publisher<T> 
{
    public static final int DEFAULT_CAPACITY = 16;
    private final AsyncArrayBlockingQueue<T>  tokens;

    public OutFlow(AsyncProc parent, int capacity) {
        super(parent);
        if (capacity < 0) {
            throw new IllegalArgumentException();
        }
        tokens = new AsyncArrayBlockingQueue<>(parent.getParent(), capacity);
    }

    public OutFlow(AsyncProc parent) {
        this(parent, DEFAULT_CAPACITY);
    }

    @Override
    public void onNext(T message) {
        tokens.add(message);
    }

    @Override
    public void onComplete() {
        tokens.onComplete();
    }

    @Override
    public void onError(Throwable ex) {
        tokens.onError(ex);
    }

    @Override
    public void subscribe(Subscriber<? super T> s) {
        tokens.subscribe(s);
    }

    public T remove() {
        return tokens.remove();
    }
}
