package org.df4j.core.util.invoker;

import java.util.function.BiFunction;

public class BiFunctionInvoker<U,V,R> extends AbstractInvoker<BiFunction<U,V,R>, R> {

    public BiFunctionInvoker(BiFunction<U, V, R> function) {
        super(function);
    }

    public R apply(Object... args) {
        assert args.length == 2;
        return function.apply((U) args[0], (V) args[1]);
    }

}
