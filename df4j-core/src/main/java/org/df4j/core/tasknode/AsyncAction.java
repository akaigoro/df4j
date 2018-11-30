package org.df4j.core.tasknode;

import org.df4j.core.simplenode.messagescalar.CompletablePromise;
import org.df4j.core.util.ActionCaller;
import org.df4j.core.util.invoker.Invoker;

import java.util.concurrent.Executor;

/**
 * this class contains components, likely useful in each async task node:
 *  - control pin -- requires start()
 *  - action caller -- allows @Action annotation
 *  - scalar result. Even if this action will produce a stream of results or no result at all,
 *  it can be used as a channel for unexpected errors.
 */
public class AsyncAction extends AsyncProc {

    protected Invoker actionCaller;
    protected final CompletablePromise<?> result = new CompletablePromise<>(this);
    /**
     * blocked initially, until {@link #start} called.
     * blocked when this actor goes to executor, to ensure serial execution of the act() method.
     */
    private Lock controlLock = new Lock();
    /**
     * cannot be restarted
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

    public CompletablePromise<?> asyncResult() {
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
        blockStarted();
    }

    private synchronized Object[] consumeTokens() {
        locks.forEach(lock -> lock.purge());
        Object[] args = new Object[asyncParams.size()];
        for (int k = 0; k< asyncParams.size(); k++) {
            AsyncParam asyncParam = asyncParams.get(k);
            args[k] = asyncParam.next();
        }
        return args;
    }

    public String toString() {
        return super.toString() + result.toString();
    }

    protected Object runAction() throws Throwable {
        if (actionCaller == null) {
            actionCaller = ActionCaller.findAction(this, asyncParams.size());
        }
        Object[] args = consumeTokens();
        Object  res = actionCaller.apply(args);
        return res;
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
