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

}
