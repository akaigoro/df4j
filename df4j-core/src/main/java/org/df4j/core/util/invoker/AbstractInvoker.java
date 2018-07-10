package org.df4j.core.util.invoker;

public abstract class AbstractInvoker<FT, R> implements Invoker<R> {
    protected final FT function;

    protected AbstractInvoker(FT function) {
        this.function = function;
    }

    @Override
    public boolean isEmpty() {
        return function == null;
    }

    public abstract R apply(Object... args) throws Exception;
}
