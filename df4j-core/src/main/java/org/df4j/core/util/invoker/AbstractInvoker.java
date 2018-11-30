package org.df4j.core.util.invoker;

public abstract class AbstractInvoker<FT> implements Invoker {
    protected final FT function;

    protected AbstractInvoker(FT function) {
        this.function = function;
    }

    @Override
    public boolean isEmpty() {
        return function == null;
    }

    public abstract Object apply(Object... args) throws Exception;
}
