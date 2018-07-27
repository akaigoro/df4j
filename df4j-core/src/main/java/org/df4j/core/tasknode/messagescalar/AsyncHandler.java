package org.df4j.core.tasknode.messagescalar;

import org.df4j.core.boundconnector.messagescalar.ConstInput;
import org.df4j.core.boundconnector.messagescalar.ScalarSubscriber;
import org.df4j.core.tasknode.Action;
import org.df4j.core.util.Pair;

import java.util.function.*;

public class AsyncHandler<T,R> extends AsyncSupplier<R> implements ScalarSubscriber<T> {
    private final ConstInput<Pair<T, Throwable>> argument = new ConstInput<>(this);
    private final BiFunction<? super T, ? super Throwable, ? extends R> action;

    public AsyncHandler(BiFunction<? super T, ? super Throwable, ? extends R> action) {
        this.action = action;
    }

    @Override
    public boolean complete(T message) {
        return  argument.complete(new Pair(message, null));
    }

    @Override
    public boolean completeExceptionally(Throwable throwable) {
        return argument.complete(new Pair(null, throwable));
    }

    @Action
    public R act(Pair<T, Throwable> arg) {
        return action.apply(arg.t, arg.u);
    }

}
