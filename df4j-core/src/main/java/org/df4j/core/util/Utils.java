package org.df4j.core.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.concurrent.Executor;

public class Utils {
    public static final Executor directExec = (Runnable r)->r.run();
    public static final Executor newThreadExec = (Runnable r)->new Thread(r).start();

    public static <E extends Throwable> void sneakyThrow(Throwable e) throws E {
        throw (E) e;
    }

    public static class CurrentThreadExecutor implements Executor {
        ArrayDeque<Runnable> stack = new ArrayDeque<>();

        @Override
        public synchronized void execute(@NotNull Runnable command) {
            stack.addFirst(command);
        }

        @Nullable
        private synchronized Runnable poll() {
            return stack.poll();
        }

        public void executeAll() {
            Runnable command;
            while ((command = poll()) != null) {
                try {
                    command.run();
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }
        }

        public void executeAll(long timeout) throws InterruptedException {
            do {
                executeAll();
                Thread.sleep(timeout);
            } while (stack.size() > 0);
        }
    }
}
