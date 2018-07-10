package org.df4j.core.util.invoker;

import java.util.function.Function;

public class FunctionInvoker<U,R> extends AbstractInvoker<Function<U,R>, R> {

    public FunctionInvoker(Function<U, R> function) {
        super(function);
    }

    public R apply(Object... args) {
        assert args.length == 1;
        return function.apply((U) args[0]);
    }

}
