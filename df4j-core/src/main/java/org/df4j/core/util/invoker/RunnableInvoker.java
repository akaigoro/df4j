package org.df4j.core.util.invoker;

public class RunnableInvoker<R> extends AbstractInvoker<Runnable, R> {

    public RunnableInvoker(Runnable runnable) {
        super(runnable);
    }

    public R apply(Object... args) {
        assert args.length == 0;
        function.run();
        return null;
    }

}
