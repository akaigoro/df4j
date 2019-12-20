package main.java.org.df4j.fancy.invoker;

public class RunnableInvoker<R> extends AbstractInvoker<Runnable> {

    public RunnableInvoker(Runnable runnable) {
        super(runnable);
    }

    public R apply(Object... args) {
        assert args.length == 0;
        function.run();
        return null;
    }

}
