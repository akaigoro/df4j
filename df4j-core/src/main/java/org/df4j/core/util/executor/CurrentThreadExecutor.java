package org.df4j.core.util.executor;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.Executor;

public class CurrentThreadExecutor implements Executor {

    boolean first = true;
    Queue<Runnable> queue;

    @Override
    public void execute(Runnable command) {
        if (first) {
            first = false;
            command.run();
            if (queue != null) {
                while ((command = queue.poll()) != null) {
                    try {
                        command.run();
                    } catch (Throwable e) {
                        e.printStackTrace();
                    }
                }
            }
            first = true;
        } else {
            if (queue == null) {
                queue = new ArrayDeque<>();
            }
            queue.add(command);
        }
    }
}
