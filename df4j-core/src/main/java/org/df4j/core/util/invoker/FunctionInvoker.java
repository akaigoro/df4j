package org.df4j.core.util.invoker;

import java.util.function.Function;

public class FunctionInvoker<U,R> extends AbstractInvoker<Function<U,R>> {

    public FunctionInvoker(Function<U, R> function) {
        super(function);
    }

    public R apply(Object... args) {
        assert args.length == 1;
        return function.apply((U) args[0]);
    }

    @Override
    public boolean returnsValue() {
        return true;
    }

}
