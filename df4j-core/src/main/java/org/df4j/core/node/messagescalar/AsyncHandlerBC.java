package org.df4j.core.node.messagescalar;

import java.util.function.BiConsumer;

public class AsyncHandlerBC<T> extends AsyncHandler<T,T>{

    public AsyncHandlerBC(BiConsumer<? super T, ? super Throwable> handler) {
        super((arg, ex)->{handler.accept(arg, ex);return arg;});
    }
}
