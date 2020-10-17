package org.df4j.core.util;

import org.df4j.core.dataflow.ClassicActor;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.Executor;

public class Utils {
    public static final Executor directExec = Runnable::run;
    public static final Executor newThreadExec = (Runnable r)->new Thread(r).start();

    public static <E extends Throwable> void sneakyThrow(Throwable e) throws E {
        throw (E) e;
    }

    public static class ZeroThreadedExecutor extends ClassicActor<Runnable> implements Executor {
        {
            start();
        }

        @Override
        public void execute(@NotNull Runnable command) {
            super.onNext(command);
        }

        @Override
        protected void runAction(Runnable command) throws Throwable {
            command.run();
        }
    }
}
