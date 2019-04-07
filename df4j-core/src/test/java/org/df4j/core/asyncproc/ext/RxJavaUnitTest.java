package org.df4j.core.asyncproc.ext;

import io.reactivex.functions.Consumer;
import org.df4j.core.asyncproc.ext.AsyncBiFunctionRx;
import org.junit.Test;

import io.reactivex.Observable;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BiFunction;

import static org.junit.Assert.assertEquals;

public class RxJavaUnitTest {

    // Simple subscription to a fixed values
    @Test
    public void returnAValue() throws InterruptedException, ExecutionException, TimeoutException {
        BiFunction<? super Integer, ? super Integer, ? extends Double> piph = (a, b) -> Math.sqrt(a*a + b*b);
        AsyncBiFunctionRx<Integer, Integer, Double> asyncPiph = new AsyncBiFunctionRx<Integer, Integer, Double>(piph);

        Consumer<Integer> rxparam1 = asyncPiph.rxparam1;
        Observable.just(3).subscribe(rxparam1);
        Observable.just(4).subscribe(asyncPiph.rxparam2);
        assertEquals(Double.valueOf(5), asyncPiph.asyncResult().get(1, TimeUnit.SECONDS), 0.0001);
    }

}