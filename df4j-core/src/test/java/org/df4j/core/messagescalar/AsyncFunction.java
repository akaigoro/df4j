package org.df4j.core.messagescalar;

import org.df4j.core.connector.messagescalar.ConstInput;
import org.df4j.core.connector.messagescalar.ScalarPublisher;
import org.df4j.core.connector.messagescalar.ScalarSubscriber;
import org.df4j.core.node.Action;
import org.df4j.core.node.AsyncTask;
import org.df4j.core.node.messagescalar.SimplePromise;
import org.df4j.core.util.SameThreadExecutor;

public abstract class AsyncFunction<R> extends AsyncTask implements ScalarPublisher<R> {
    protected SimplePromise<R> result = new SimplePromise<>();
    {
        setExecutor(new SameThreadExecutor());
    }

    @Override
    public <S extends ScalarSubscriber<? super R>> S subscribe(S subscriber) {
        result.subscribe(subscriber);
        return subscriber;
    }

    protected void setResult(R res) {
        result.post(res);
    }

    protected void setResultAsync(ScalarPublisher<R> publisher) {
        publisher.subscribe(result);
    }

    public static abstract class UnaryAsyncFunction<T,R> extends AsyncFunction<R> {
        ConstInput<T> arg = new ConstInput<>(this);

        protected UnaryAsyncFunction(ScalarPublisher<T> pa) {
            pa.subscribe(arg);
        }

        @Action
        protected void act(T argValue) {
            R res = apply(argValue);
            setResult(res);
        }

        protected abstract R apply(T current);
    }

    public static abstract class BinaryAsyncFunction<T,U,R> extends AsyncFunction<R> {
        ConstInput<T> arg1 = new ConstInput<>(this);
        ConstInput<U> arg2 = new ConstInput<>(this);
        {
            start();
        }

        public BinaryAsyncFunction() {
        }

        protected BinaryAsyncFunction(ScalarPublisher<T> pa, ScalarPublisher<U> pb) {
            pa.subscribe(arg1);
            pb.subscribe(arg2);
        }

        @Action
        public void act(T arg1, U arg2) {
            R res = apply(arg1, arg2);
            setResult(res);
        }

        protected abstract R apply(T val1, U val2);
    }
}

