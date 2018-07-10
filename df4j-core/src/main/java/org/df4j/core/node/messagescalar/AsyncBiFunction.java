package org.df4j.core.node.messagescalar;

import org.df4j.core.connector.messagescalar.ConstInput;
import org.df4j.core.node.Action;
import org.df4j.core.node.AsyncActionTask;
import org.df4j.core.node.AsyncResult;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;

public class AsyncBiFunction<U, V, R> extends AsyncActionTask<R> {
    public final ConstInput<U> arg1 = new ConstInput<>(this);
    public final ConstInput<V> arg2 = new ConstInput<>(this);
    @Action
    protected final BiFunction<? super U,? super V,? extends R> fn;

    public AsyncBiFunction(BiFunction<? super U, ? super V, ? extends R> fn) {
        this.fn = fn;
    }

    public AsyncBiFunction(BiConsumer<? super U, ? super V> action) {
        this.fn = (a1, a2)->{action.accept(a1, a2); return null;};
    }

    public AsyncBiFunction(Runnable action) {
        this.fn = (a1, a2)->{action.run(); return null;};
    }

}
