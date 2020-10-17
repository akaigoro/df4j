package org.df4j.core.port;

import org.df4j.core.dataflow.AsyncProc;
import org.df4j.protocol.SignalFlow;
import org.reactivestreams.Subscription;

import java.util.TimerTask;

/**
 * asynchronous receiver of permit flow from a {@link SignalFlow.Publisher}, e.g. {@link org.df4j.core.connector.AsyncSemaphore}.
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
        super(parent, false);
    }

    /**
     * @param parent {@link AsyncProc} to which this port belongs
     * @param permits initial number of permits; can be negative
     */
    public InpSignal(AsyncProc parent, long permits) {
        super(parent, permits > 0);
        this.permits = permits;
    }

    @Override
    public void onSubscribe(Subscription subscription) {
        synchronized(parent) {
            this.subscription = subscription;
        }
    }

    @Override
    public  void release(long n) {
        synchronized(parent) {
            boolean wasBlocked = !isReady();
            permits += n;
            if (wasBlocked && permits > 0) {
                unblock();
            }
        }
    }

    /**
     * Reduces the number of permits.
     * Analogue of {@link InpFlow#remove()} and {@link java.util.concurrent.Semaphore#acquire(int)}
     */
    public void remove() {
        synchronized(parent) {
            boolean wasReady = isReady();
            permits--;
            if (wasReady && permits == 0) {
                block();
            }
        }
    }

    public void request(long n) {
        if (n <= 0) {
            throw new IllegalArgumentException();
        }
        Subscription subs;
        synchronized(parent) {
            subs = this.subscription;
            if (subs == null) {
                throw new IllegalStateException();
            }
        }
        subs.request(n);
    }


    /**
     * analogue od {@link InpFlow#remove()}
     */
    public void acquireAndRequest() {
        Subscription subs;
        synchronized(parent) {
            boolean wasReady = isReady();
            permits--;
            if (wasReady && permits == 0) {
                block();
            }
            subs = this.subscription;
            if (subs == null) {
                throw new IllegalStateException();
            }
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
