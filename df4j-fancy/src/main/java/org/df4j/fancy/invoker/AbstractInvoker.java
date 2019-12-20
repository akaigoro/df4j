package main.java.org.df4j.fancy.invoker;

public abstract class AbstractInvoker<FT> implements Invoker {
    protected final FT function;

    protected AbstractInvoker(FT function) {
        if (function == null) {
            throw new IllegalArgumentException();
        }
        this.function = function;
    }

    public abstract Object apply(Object... args) throws Exception;
}
