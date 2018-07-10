package org.df4j.core.node;

import org.df4j.core.util.ActionCaller;

import java.util.concurrent.Executor;

/**
 * this class contains components, likely useful in each async node:
 *  - control pin
 *  - action Caller
 *
 * @param <R>
 */
public class AsyncTask<R> extends AsyncTaskBase {

    private ActionCaller<R> actionMethod;
    protected volatile boolean started = false;
    protected volatile boolean stopped = false;

    /**
     * blocked initially, until {@link #start} called.
     * blocked when this actor goes to executor, to ensure serial execution of the act() method.
     */
    protected ControlLock controlLock = new ControlLock();

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
        Object[] args = new Object[connectors.size()];
        for (int k=0; k<connectors.size(); k++) {
            AsyncTaskBase.Connector connector = connectors.get(k);
            args[k] = connector.next();
        }
        return args;
    }

    protected R runAction() throws Exception {
        controlLock.turnOff();
        if (actionMethod == null) {
            try {
                actionMethod = new ActionCaller(this, connectors.size());
            } catch (NoSuchMethodException e) {
                throw new IllegalStateException(e);
            }
        }
        Object[] args = consumeTokens();
        R  res = actionMethod.apply(args);
        return res;
    }

    @Override
    public void run() {
        try {
            R res = runAction();
        } catch (Throwable e) {
            stop();
        }
    }

    protected class ControlLock extends Lock {
    }

}
