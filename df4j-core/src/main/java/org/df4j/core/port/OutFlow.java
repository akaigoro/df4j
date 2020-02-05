package org.df4j.core.port;

import org.df4j.core.base.OutFlowBase;
import org.df4j.core.dataflow.AsyncProc;
import org.df4j.protocol.Flow;
import org.reactivestreams.Subscriber;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

/**
 * A passive source of messages (like a server).
 * Unblocked initially.
 * It has room for single message.
 * Blocked when overflow.
 *
 * Because of complex logic, it is designed as an Actor itself. However, it still controls firing of the parent actor.
 */

/**
 * is ready when has room to store at least one toke
 * @param <T>
 */
public class OutFlow<T> extends AsyncProc.Port implements OutMessagePort<T>, Flow.Publisher<T> {
    final private OutFlowBase<T> base;

    public OutFlow(AsyncProc parent, int capacity) {
        parent.super(true);
        base = new MyOutFlowBase(plock, capacity);
    }

    public OutFlow(AsyncProc parent, int capacity, OutFlowBase<T> base) {
        parent.super(true);
        this.base = base;
    }

    public OutFlow(AsyncProc parent) {
        this(parent, 16);
    }

    public boolean isCompleted() {
        return base.isCompleted();
    }

    @Override
    public void subscribe(Subscriber<? super T> subscriber) {
        base.subscribe(subscriber);
    }

    /**
     *
     * @param token token to insert
     * @return true if token inserted
     */
    @Override
    public void onNext(T token) {
        if (!base.offer(token)) {
            throw new IllegalStateException("buffer overflow");
        }
    }

    public void onError(Throwable cause) {
        base.onError(cause);
    }

    public void onComplete() {
        base.onComplete();
    }

    public T poll() {
        return base.poll();
    }

    public T poll(long timeout, TimeUnit unit) throws InterruptedException {
        return base.poll(timeout, unit);
    }

    public T take() throws InterruptedException {
        return base.take();
    }

    private class MyOutFlowBase extends OutFlowBase<T> {
        public MyOutFlowBase(Lock plock, int capacityP) {
            super(plock, capacityP);
        }

        @Override
        protected void hasRoomEvent() {
            unblock();
        }

        @Override
        protected void noRoomEvent() {
            block();
        }
    }
}
