package org.df4j.core.asyncproc.rxjava2;

import io.reactivex.Single;
import io.reactivex.SingleObserver;
import io.reactivex.SingleSource;
import io.reactivex.disposables.Disposable;
import org.df4j.core.asyncproc.ScalarResult;
import org.df4j.core.asyncproc.ScalarSubscriber;
import org.junit.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.Assert.assertEquals;

public class RxJavaUnitTest {

    /**
     *  * pass data from rxjava2 to df4j:
     *  *   {@link io.reactivex.SingleSource} source;
     *  *   {@link org.df4j.core.asyncproc.ScalarSubscriber} subscriber;
     *  *   source.suscribe(subscriber.{@link org.df4j.core.asyncproc.ScalarSubscriber#asSingleObserver()};
     * @throws InterruptedException
     * @throws ExecutionException
     * @throws TimeoutException
     */
    @Test
    public void fromRxJava() throws InterruptedException, ExecutionException, TimeoutException {
        final int val = 3;
        SingleSource<Integer> rxJavaSource = Single.just(val);
        ScalarResult<Integer> scalarResult = new ScalarResult<>();
        SingleObserver<Integer> observer = new SingleObserverAdapter(scalarResult);
        rxJavaSource.subscribe(observer);
        int res = scalarResult.get(200, TimeUnit.MILLISECONDS);
        assertEquals("", val, res);
    }

    /**
     * demonstrated interoperability with rxjava2 objects:
     *
     * pass data from df4j to rxjava2:
     *   {@link org.df4j.core.asyncproc.ScalarPublisher} source;
     *   {@link io.reactivex.SingleObserver} sink;
     *   source.suscribe{sink);
     *
     */
    @Test
    public void toRxJava() throws InterruptedException, ExecutionException, TimeoutException {
        final int val = 3;
        ScalarResult<Integer> scalarResult = new ScalarResult<>();
        CompletableObserver<Integer> rxJavaSink = new CompletableObserver<>();
        ScalarSubscriber<? super Integer> s = new ScalarSubscriberAdapter<>((SingleObserver)rxJavaSink);
        scalarResult.subscribe(s);
        scalarResult.onComplete(val);
        int res = rxJavaSink.get(200, TimeUnit.MILLISECONDS);
        assertEquals("", val, res);
    }

    static class CompletableObserver<T> extends CompletableFuture<T> implements SingleObserver<T>{

        @Override
        public void onSubscribe(Disposable d) {
        }

        @Override
        public void onSuccess(T t) {
            super.complete(t);
        }

        @Override
        public void onError(Throwable e) {
            super.completeExceptionally(e);
        }
    }

}