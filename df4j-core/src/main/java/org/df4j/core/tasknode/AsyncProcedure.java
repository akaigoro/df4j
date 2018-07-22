package org.df4j.core.tasknode;

import org.df4j.core.util.ActionCaller;
import org.df4j.core.util.invoker.Invoker;

import java.util.concurrent.Executor;

/**
 * this class contains components, likely useful in each async task node:
 *  - control pin -- requires start()
 *  - action caller -- allows @Action annotation
 *
 * @param <R>
 */
public class AsyncProcedure<R> extends AsyncTask {

    protected Invoker<R> actionCaller;
    protected volatile boolean started = false;
    protected volatile boolean stopped = false;

    /**
     * blocked initially, until {@link #start} called.
     * blocked when this actor goes to executor, to ensure serial execution of the act() method.
     */
    protected Lock controlLock = new Lock();

    public AsyncProcedure() {
    }

    public AsyncProcedure(Invoker<R> actionCaller) {
        this.actionCaller = actionCaller;
    }

    public boolean isStarted() {
        return started;
    }

    public synchronized void start() {
        if (stopped) {
            return;
        }
        started = true;
        controlLock.turnOn();
    }

    public synchronized void start(Executor executor) {
        setExecutor(executor);
        start();
    }

    public synchronized void stop() {
        stopped = true;
        controlLock.turnOff();
    }

    public synchronized Object[] consumeTokens() {
        if (!isStarted()) {
            throw new IllegalStateException("not started");
        }
        locks.forEach(lock -> lock.purge());
        Object[] args = new Object[asynctParams.size()];
        for (int k = 0; k< asynctParams.size(); k++) {
            AsynctParam asynctParam = asynctParams.get(k);
            args[k] = asynctParam.next();
        }
        return args;
    }

    protected R runAction() throws Exception {
        if (actionCaller == null) {
            try {
                actionCaller = ActionCaller.findAction(this, asynctParams.size());
            } catch (NoSuchMethodException e) {
                throw new IllegalStateException(e);
            }
        }
        Object[] args = consumeTokens();
        R  res = actionCaller.apply(args);
        return res;
    }

    @Override
    public void run() {
        try {
            controlLock.turnOff();
            runAction();
        } catch (Throwable e) {
            stop();
        }
    }
}
