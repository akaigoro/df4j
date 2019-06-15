package org.df4j.core.tutorial.actor;

import org.df4j.core.actor.Actor;
import org.df4j.core.actor.StreamInput;
import org.df4j.core.actor.StreamOutput;
import org.df4j.core.asyncproc.ScalarResult;
import org.df4j.core.asyncproc.ext.AsyncBiFunction;
import org.df4j.core.asyncproc.ext.AsyncFunction;
import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BiFunction;
import java.util.function.Function;

public class SumSquareTest {

    public static class SumSquare extends Actor {
        final StreamInput<Double> paramX = new StreamInput<>(this);
        final StreamInput<Double> paramY = new StreamInput<>(this);
        final StreamOutput<Double> out = new StreamOutput<>(this);

        {
            start();
        }

        @Override
        protected void runAction() throws Throwable {
            Double x = paramX.current();
            Double y = paramY.current();
            double res = Math.sqrt(x*x+y*y);
            out.onNext(res);
        }
    }

    @Test
    public void test() throws ExecutionException, InterruptedException, TimeoutException {
        // create 3 nodes
        SumSquare ssq = new SumSquare();
        ssq.paramX.onNext(3.0);
        ssq.paramX.onNext(5.0);
        ssq.paramY.onNext(4.0);
        ssq.paramY.onNext(12.0);
        // get the results
        Assert.assertEquals(5, ssq.out.take(1, TimeUnit.SECONDS), 0e-6);
        Assert.assertEquals(13, ssq.out.take(1, TimeUnit.SECONDS), 0e-6);
    }

    /**5, 12, 13
     * computes arithmetic expression sum = 3*3 + 4*4
     * @throws ExecutionException
     * @throws InterruptedException
     * @throws TimeoutException
     */
    @Test
    public void testDFF() throws ExecutionException, InterruptedException, TimeoutException {
        Function<Integer, Integer> square = arg -> arg * arg;
        BiFunction<Integer, Integer, Integer> plus = (argX, argY) -> argX + argY;
        // create nodes and connect them
        AsyncFunction<Integer, Integer> sqX = new AsyncFunction<>(square);
        AsyncFunction<Integer, Integer> sqY = new AsyncFunction<>(square);
        AsyncBiFunction<Integer, Integer, Integer> sum = new AsyncBiFunction<Integer, Integer, Integer>(plus);
        // make 2 connections
        sqX.subscribe(sum.param1);
        sqY.subscribe(sum.param2);
        // provide input information:
        sqX.onComplete(3);
        sqY.onComplete(4);
        // get the result
        ScalarResult<Integer> result = sum.asyncResult();
        int res = result.get(1, TimeUnit.SECONDS);
        Assert.assertEquals(25, res);
    }

    @Test
    public void testCF() throws ExecutionException, InterruptedException, TimeoutException {
        Function<Integer, Integer> square = arg -> arg * arg;
        BiFunction<Integer, Integer, Integer> plus = (argX, argY) -> argX + argY;
        // create nodes and connect them
        CompletableFuture<Integer> sqXParam = new CompletableFuture();
        CompletableFuture<Integer> sqYParam = new CompletableFuture();
        CompletableFuture<Integer> sum = sqXParam
                .thenApply(square)
                .thenCombine(sqYParam.thenApply(square),
                        plus);
        // provide input information:
        sqXParam.complete(3);
        sqYParam.complete(4);
        // get the result
        int res = sum.get(1, TimeUnit.SECONDS);
        Assert.assertEquals(25, res);
    }

}
