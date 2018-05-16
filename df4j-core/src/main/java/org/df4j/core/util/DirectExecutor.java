package org.df4j.core.util;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.Executor;

public class DirectExecutor implements Executor {

    @Override
    public void execute(Runnable command) {
        command.run();
    }
}
