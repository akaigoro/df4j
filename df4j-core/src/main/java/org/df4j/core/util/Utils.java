package org.df4j.core.util;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.Executor;

public class Utils {
    public static final Executor directExec = (Runnable r)->r.run();
    public static final Utils.CurrentThreadExecutor currentThreadExec = new Utils.CurrentThreadExecutor();
    public static final Executor newThreadExec = (Runnable r)->new Thread(r).start();

    public static <E extends Throwable> void sneakyThrow(Throwable e) throws E {
        throw (E) e;
    }

    /**
     * for debugging
     * Serial executor working on current thread
     * <code>
     *   {@link CurrentThreadExecutor executor} = new {@link CurrentThreadExecutor ()};
     *   actor.setExecutor(executor);
     *   actor.start();
     *   executor.runAll();
     * </code>
     */
    public static class CurrentThreadExecutor implements Executor {

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
}
