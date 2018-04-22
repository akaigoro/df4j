package org.df4j.core.impl.messagescalar;


import org.df4j.core.spi.messagescalar.Promise;

import java.util.function.Function;

public class UnaryPromiseFunc<T,R> extends PromiseFunc<R> {
    Function<? super T,? extends R> fn;
    ConstInput<T> a = new ConstInput<>();

    UnaryPromiseFunc(Function<? super T,? extends R> fn, Promise<T> pa) {
        this.fn = fn;
        pa.postTo(a);
    }

    @Override
    public void act() {
        R res = fn.apply(a.get());
        out.post(res);
    }
}

