package org.df4j.core.util;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.Executor;

public class Utils {
    public static final Executor directExec = Runnable::run;
    public static final Executor newThreadExec = (Runnable r)->new Thread(r).start();

    public static <E extends Throwable> void sneakyThrow(Throwable e) throws E {
        throw (E) e;
    }

}
