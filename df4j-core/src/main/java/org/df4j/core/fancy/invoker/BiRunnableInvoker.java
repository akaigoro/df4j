package org.df4j.core.fancy.invoker;

public class BiRunnableInvoker<U,V> extends AbstractInvoker<Runnable> {

    public BiRunnableInvoker(Runnable runnable) {
        super(runnable);
    }

    public Void apply(Object... args) {
        assert args.length == 2;
        function.run();
        return null;
    }

}
