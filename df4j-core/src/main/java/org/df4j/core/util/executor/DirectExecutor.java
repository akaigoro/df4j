package org.df4j.core.util.executor;

import java.util.concurrent.Executor;

public class DirectExecutor implements Executor {
    public static final DirectExecutor directExecutor = new DirectExecutor();

    @Override
    public void execute(Runnable command) {
        command.run();
    }
}
