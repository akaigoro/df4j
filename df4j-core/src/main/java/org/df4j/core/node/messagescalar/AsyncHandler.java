package org.df4j.core.node.messagescalar;

import org.df4j.core.connector.messagescalar.ConstInput;
import org.df4j.core.connector.messagescalar.ScalarSubscriber;
import org.df4j.core.node.Action;
import org.df4j.core.util.Pair;

import java.util.function.*;

public class AsyncHandler<T,R> extends AsyncResult<R> implements ScalarSubscriber<T> {
    private final ConstInput<Pair<T, Throwable>> argument = new ConstInput<>(this);
    private final BiFunction<? super T, ? super Throwable, ? extends R> handler;

    public AsyncHandler(BiFunction<? super T, ? super Throwable, ? extends R> handler) {
        this.handler = handler;
    }

    @Override
    public void post(T message) {
        argument.post(new Pair(message, null));
    }

    @Override
    public void postFailure(Throwable throwable) {
        argument.post(new Pair(null, throwable));
    }

    @Action
    public void act(Pair<T, Throwable> arg) {
        R res = handler.apply(arg.t, arg.u);
        complete(res);
    }

}
