package org.df4j.core.fancy.invoker;

import java.util.function.Supplier;

public class SupplierInvoker<R> extends AbstractInvoker<Supplier<R>> {


    public SupplierInvoker(Supplier<R> supplier) {
        super(supplier);
    }

    public R apply(Object... args) {
        assert args.length == 0;
        return function.get();
    }

}
