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

    protected void blockStarted() {
        controlLock.block();
    }

    public synchronized void stop() {
        stopped = true;
        if (!result.isDone()) {
            result.onComplete();
        }
    }

    protected abstract void runAction() throws Throwable;

    protected void run() {
        try {
            blockStarted();
            runAction();
            if (!isStopped()) {
                purgeAll();
                start(); // restart execution
            }
        } catch (Throwable e) {
            result.completeExceptionally(e);
            stop();
        }
    }
}
