package main.java.org.df4j.fancy;

import java.util.concurrent.Executor;

/**
 * works like a single-threaded Executor, but does not own a thread.
 */
public class SerialExecutor extends Hactor<Runnable> implements Executor {

    public SerialExecutor() {
        start();
    }

    public SerialExecutor(Executor executor) {
        setExecutor(executor);
        start();
    }

    @Override
    public void execute(Runnable command) {
        super.onNext(command);
    }

    @Override
    protected void runAction(Runnable arg) {
        arg.run();
    }
}
