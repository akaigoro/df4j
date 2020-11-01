package org.df4j.core.port;

import org.df4j.core.connector.AsyncSemaphore;
import org.df4j.core.actor.AsyncProc;
import org.df4j.protocol.SignalFlow;
import org.reactivestreams.Subscription;

/**
 * asynchronous receiver of permit flow from a {@link SignalFlow.Publisher}, e.g. {@link AsyncSemaphore}.
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
    public synchronized void onSubscribe(Subscription subscription) {
        this.subscription = subscription;
    }

    @Override
    public synchronized void release(long n) {
        boolean wasBlocked = !isReady();
        permits += n;
        if (wasBlocked && permits > 0) {
            unblock();
        }
    }

    /**
     * Reduces the number of permits.
     * Analogue of {@link InpFlow#remove()} and {@link java.util.concurrent.Semaphore#acquire(int)}
     */
    public synchronized void remove() {
        if (permits <= 0) {
            throw new IllegalStateException("no avalable permits");
        }
        boolean wasReady = isReady();
        permits--;
        if (wasReady && permits == 0) {
            block();
        }
    }
}
