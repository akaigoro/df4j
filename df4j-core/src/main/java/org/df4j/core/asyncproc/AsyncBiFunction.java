package org.df4j.core.asyncproc;

import org.df4j.core.util.invoker.BiConsumerInvoker;
import org.df4j.core.util.invoker.BiFunctionInvoker;
import org.df4j.core.util.invoker.BiRunnableInvoker;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;

public class AsyncBiFunction<U, V, R> extends AsyncSupplier<R> {
    public final ScalarInput<U> param1 = new ScalarInput<>(this);
    public final ScalarInput<V> param2 = new ScalarInput<>(this);

    public AsyncBiFunction(BiFunction<? super U, ? super V, ? extends R> fn) {
        super(new BiFunctionInvoker(fn));
    }

    public AsyncBiFunction(BiConsumer<U, V> action) {
        super(new BiConsumerInvoker(action));
    }

    public AsyncBiFunction(Runnable runnable) {
        super(new BiRunnableInvoker(runnable));
    }
}
