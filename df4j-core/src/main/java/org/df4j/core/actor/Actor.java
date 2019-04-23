package org.df4j.core.actor;

import org.df4j.core.asyncproc.AsyncProc;
import org.df4j.core.asyncproc.ScalarLock;

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
    private ControlPin controlLock = new ControlPin();
    /**
     * if true, this action cannot be restarted
     */
    private volatile boolean stopped = false;

    public boolean isStopped() {
        return stopped;
    }

    public void start() {
        synchronized(this) {
            if (stopped) {
                return;
            }
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
            boolean allCompleted = true;
            for (int k = 0; k < pins.size(); k++) {
                ScalarLock pin = pins.get(k);
                pin.moveNext();
                if (pin != controlLock) {
                    allCompleted &= pin.isCompleted();
                }
            }
            // when all the Pins except for the controlLock are completed,
            // do not restart execution to avoid infinite loop.
            if (allCompleted) {
                stop();
                return;
            }
            start();
        } catch (Throwable e) {
            stopExceptionally(e);
        }
    }

    protected class ControlPin extends StreamLock {
        public ControlPin() {
            super(Actor.this);
        }
    }
}
