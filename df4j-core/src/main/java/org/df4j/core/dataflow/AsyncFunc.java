package org.df4j.core.dataflow;

import org.df4j.protocol.Completable;
import org.df4j.protocol.Scalar;
import org.df4j.protocol.SimpleSubscription;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public abstract class AsyncFunc<R> extends AsyncProc  implements Scalar.Source<R>, Future<R> {
    private R result;

    public AsyncFunc(Dataflow parent) {
        super(parent);
    }

    public AsyncFunc() {
        super();
    }

    @Override
    public void subscribe(Scalar.Observer<? super R> observer) {
        ValueSubscription subscription = new ValueSubscription(observer);
        super.subscribe(subscription);
    }

    @Override
    protected void runAction() throws Throwable {
        result = callAction();
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return false;
    }

    @Override
    public boolean isCancelled() {
        return false;
    }

    @Override
    public boolean isDone() {
        return isCompleted();
    }

    @Override
    public R get() throws InterruptedException, ExecutionException {
        super.join();
        return result;
    }

    @Override
    public R get(long timeout, @NotNull TimeUnit unit) throws TimeoutException {
        if (super.blockingAwait(timeout, unit)) {
            return result;
        } else {
            throw new TimeoutException();
        }
    }

    protected abstract R callAction() throws Throwable;

    class ValueSubscription extends CompletionSubscription implements Completable.Observer {
        Scalar.Observer<? super R> subscriber;

        protected ValueSubscription(Scalar.Observer<? super R> subscriber) {
            this.subscriber = subscriber;
        }

        @Override
        public void onComplete() {
            subscriber.onSuccess(result);
        }

        @Override
        public void onSubscribe(SimpleSubscription subscription) {
            subscriber.onSubscribe(this);
        }

        @Override
        public void onError(Throwable e) {
            subscriber.onError(e);
        }
    }
}
