package org.df4j.core.impl.messagescalar.promisefuncs;

import org.df4j.core.spi.messagescalar.Promise;

import java.util.function.BiFunction;

public class BinaryPromiseFunc<T,U,R> extends PromiseFunc<R> {
    BiFunction<? super T,? super U,? extends R> fn;
    ConstInput<T> a = new ConstInput<>();
    ConstInput<U> b = new ConstInput<>();

    protected BinaryPromiseFunc(BiFunction<? super T, ? super U, ? extends R> fn, Promise<T> pa, Promise<U> pb) {
        this.fn = fn;
        pa.postTo(a);
        pb.postTo(b);
    }

    @Override
    public void act() {
        R res = fn.apply(a.get(), b.get());
        out.post(res);
    }
}

