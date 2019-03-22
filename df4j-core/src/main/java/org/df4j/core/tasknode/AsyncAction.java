package org.df4j.core.tasknode;

import org.df4j.core.simplenode.messagescalar.CompletablePromise;
import org.df4j.core.util.ActionCaller;
import org.df4j.core.util.invoker.Invoker;

import java.util.concurrent.Executor;

/**
 * this class contains components, likely useful in each async task node:
 *  - control pin -- prevents concurrent execution of the same AsyncAction
 *  - action caller -- allows @Action annotation
 *  - scalar result. Even if this action will produce a stream of results or no result at all,
 *  it can be used as a channel for unexpected errors.
 */
public class AsyncAction<R> extends AsyncProc {
    /**
     * blocked initially, until {@link #start} called.
     * blocked when this actor goes to executor, to ensure serial execution of the act() method.
     */
    protected Lock controlLock = new Lock();

    protected Invoker actionCaller;

    protected final CompletablePromise<R> result = new CompletablePromise<>();

    /**
     * if true, this action cannot be restarted
     */
    protected volatile boolean stopped = false;

    public AsyncAction() {
    }

    public AsyncAction(Invoker actionCaller) {
        this.actionCaller = actionCaller;
    }

    public boolean isStarted() {
        return !controlLock.isBlocked();
    }

    public boolean isStopped() {
        return stopped;
    }

    public CompletablePromise<R> asyncResult() {
        return result;
    }

    public synchronized void start() {
        if (stopped) {
            throw new IllegalStateException();
        }
        controlLock.turnOn();
    }

    public synchronized void start(Executor executor) {
        setExecutor(executor);
        start();
    }

    protected void blockStarted() {
        controlLock.turnOff();
    }

    public synchronized void stop() {
        stopped = true;
        if (!result.isDone()) {
            result.onComplete();
        }
    }

    public String toString() {
        return super.toString() + result.toString();
    }

    protected R callAction() throws Throwable {
        if (actionCaller == null) {
            actionCaller = ActionCaller.findAction(this, getParamCount());
        }
        Object[] args = collectTokens();
        R  res = (R) actionCaller.apply(args);
        return res;
    }

    protected void runAction() throws Throwable {
        callAction();
    }

    @Override
    public void run() {
        try {
            blockStarted();
            runAction();
        } catch (Throwable e) {
            result.completeExceptionally(e);
            stop();
        }
    }
}
