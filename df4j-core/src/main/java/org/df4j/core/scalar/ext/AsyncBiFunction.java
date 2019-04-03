package org.df4j.core.scalar.ext;

import org.df4j.core.util.invoker.BiConsumerInvoker;
import org.df4j.core.util.invoker.BiFunctionInvoker;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;

public class AsyncBiFunction<U, V, R> extends AsyncSupplier<R> {
    public final ConstInput<U> param1 = new ConstInput<>();
    public final ConstInput<V> param2 = new ConstInput<>();

    public AsyncBiFunction(BiFunction<? super U, ? super V, ? extends R> fn) {
        super(new BiFunctionInvoker(fn));
    }

    public AsyncBiFunction(BiConsumer<U, V> action) {
        super(new BiConsumerInvoker(action));
    }
}
