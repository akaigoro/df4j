package org.df4j.core.actor;

import org.df4j.core.asyncproc.AsyncProc;

import java.util.concurrent.Executor;

/**
 * Actor is a reusable AsyncProc: after execution, it executes again as soon as new array of arguments is ready.
 *
 * Overlapping of execution of the same instance is prevented by controlling {@link Actor#controlLock}
 */
public abstract class Actor extends AsyncProc {
    /**
     * blocked initially, until {@link #start} called.
     * blocked when this actor goes to executor, to ensure serial execution of the act() method.
     */
    private Pin controlLock = new Pin();
    /**
     * if true, this action cannot be restarted
     */
    private volatile boolean stopped = false;

    public boolean isStopped() {
        return stopped;
    }

    public synchronized void start() {
        if (stopped) {
            throw new IllegalStateException();
        }
        controlLock.unblock();
    }

    public synchronized void start(Executor executor) {
        setExecutor(executor);
        start();
    }

    protected void blockControl() {
        controlLock.block();
    }

    public synchronized void stop(Object completiontValue) {
        synchronized(this) {
            if (stopped) {
                return;
            }
            stopped = true;
        }
        result.onComplete(completiontValue);
    }

    public synchronized void stop() {
        stop(null);
    }

    public synchronized void stopExceptionally(Throwable t) {
        synchronized(this) {
            if (stopped) {
                return;
            }
            stopped = true;
        }
        result.onError(t);
    }

    protected abstract void runAction() throws Throwable;

    protected void run() {
        try {
            blockControl();
            runAction();
            if (isStopped()) {
                return;
            }
            // when all the Pins except for the controlLock are completed,
            // do not restart execution to avoid infinite loop.
            if (lockCount() == 1) { // 1 means the controlLock
                return;
            }
            nextAll();
            start();
        } catch (Throwable e) {
            stopExceptionally(e);
        }
    }
}
