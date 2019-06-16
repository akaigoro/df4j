package org.df4j.core.util.executor;

import org.df4j.core.actor.ext.Hactor;

import java.util.concurrent.Executor;

/**
 * works like a single-threaded Executor, but does not own a thread
 */
public class SerialExecutor extends Hactor<Runnable> {
    {
        start();
    }

    public SerialExecutor() {
    }

    public SerialExecutor(Executor executor) {
        setExecutor(executor);
    }

    @Override
    protected void runAction(Runnable arg) {
        arg.run();
    }
}
