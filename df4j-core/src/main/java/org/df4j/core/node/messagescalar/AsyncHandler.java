package org.df4j.core.node.messagescalar;

import org.df4j.core.connector.messagescalar.ConstInput;
import org.df4j.core.connector.messagescalar.ScalarSubscriber;

import java.util.function.*;

public class AsyncHandler<T, R> extends AsyncResultFuture<R>  implements ScalarSubscriber<T> {
    protected final ConstInput<T> argument = new ConstInput<>(this);
    protected BiFunction<? super T, Throwable, ? extends R> handler;

    public AsyncHandler() {
    }

    public AsyncHandler(BiFunction<? super T, Throwable, ? extends R> handler) {
        this.handler = handler;
    }

    public AsyncHandler(BiConsumer<? super T, ? super Throwable> action) {
        this.handler = (value, ex) -> {action.accept(value, ex); return (R) value;};
    }

    @Override
    public void post(T message) {
        argument.post(message);
    }

    @Override
    public void postFailure(Throwable throwable) {
        argument.postFailure(throwable);
    }

    public void run() {
        try {
            R res = handler.apply(argument.getValue(), argument.getException());
            result.complete(res);
        } catch (Throwable e) {
            result.completeExceptionally(e);
        }
    }

}
