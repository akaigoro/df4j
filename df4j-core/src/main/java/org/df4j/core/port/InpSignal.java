package org.df4j.core.port;

import org.df4j.core.dataflow.AsyncProc;
import org.reactivestreams.*;
import org.df4j.protocol.SignalFlow;

import java.util.TimerTask;

/**
 * asynchronous receiver of permit flow from a {@link SignalFlow.Publisher}, e.g. {@link org.df4j.core.communicator.AsyncSemaphore}.
 *
 * it is lazy, so implicit invocation of {@link InpSignal#request(long)} required
 */
public class InpSignal extends AsyncProc.Port implements SignalFlow.Subscriber {
    private Subscription subscription;
    /** the port is blocked if permits &le; 0 */
    protected long permits = 0;

    /**
     * @param parent {@link AsyncProc} to which this port belongs
     */
    public InpSignal(AsyncProc parent) {
        parent.super(false);
    }

    /**
     * @param parent {@link AsyncProc} to which this port belongs
     * @param permits initial number of permits; can be negative
     */
    public InpSignal(AsyncProc parent, long permits) {
        parent.super(permits > 0);
        this.permits = permits;
    }

    @Override
    public void onSubscribe(Subscription subscription) {
        plock.lock();
        try {
            this.subscription = subscription;
        } finally {
            plock.unlock();
        }
    }

    @Override
    public  void release(long n) {
        plock.lock();
        try {
            boolean wasBlocked = !isReady();
            permits += n;
            if (wasBlocked && permits > 0) {
                unblock();
            }
        } finally {
            plock.unlock();
        }
    }

    @Override
    public void release() {
        release(1);
    }

    /**
     * analogue od {@link InpFlow#remove()}
     */
    public void acquire() {
        plock.lock();
        try {
            boolean wasReady = isReady();
            permits--;
            if (wasReady && permits == 0) {
                block();
            }
        } finally {
            plock.unlock();
        }
    }

    public void request(long n) {
        if (n <= 0) {
            throw new IllegalArgumentException();
        }
        plock.lock();
        Subscription subs;
        try {
            subs = this.subscription;
            if (subs == null) {
                throw new IllegalStateException();
            }
        } finally {
            plock.unlock();
        }
        subs.request(n);
    }


    /**
     * analogue od {@link InpFlow#remove()}
     */
    public void acquireAndRequest() {
        Subscription subs;
        plock.lock();
        try {
            boolean wasReady = isReady();
            permits--;
            if (wasReady && permits == 0) {
                block();
            }
            subs = this.subscription;
            if (subs == null) {
                throw new IllegalStateException();
            }
        } finally {
            plock.unlock();
        }
        subs.request(1);
    }

    /**
     * awakes this port after delay
     * @param delay time delay in milliseconds
     */
    public void delayedAwake(long delay) {
        AsyncProc parent = getParent();
        if (parent.isCompleted()) {
            return;
        }
        TimerTask task = new TimerTask(){
            @Override
            public void run() {
                release();
            }
        };
        parent.getTimer().schedule(task, delay);
    }
}
