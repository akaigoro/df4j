package org.df4j.examples.tutorial.basic;

import org.df4j.core.node.messagescalar.SubscriberPromise;
import org.df4j.core.connector.messagescalar.ScalarInput;
import org.df4j.core.node.Action;
import org.df4j.core.node.AsyncProcedure;
import org.df4j.core.node.messagescalar.AsyncBiFunction;
import org.df4j.core.node.messagescalar.AsyncFunction;
import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.BiFunction;
import java.util.function.Function;

public class SumSquareTest {
    public static class Square extends AsyncProcedure {
        final SubscriberPromise<Integer> result = new SubscriberPromise<>();
        final ScalarInput<Integer> param = new ScalarInput<>(this);

        @Action
        public void compute(Integer arg) {
            int res = arg*arg;
            result.complete(res);
        }
    }

    public static class Sum extends AsyncProcedure {
        final SubscriberPromise<Integer> result = new SubscriberPromise<>();
        final ScalarInput<Integer> paramX = new ScalarInput<>(this);
        final ScalarInput<Integer> paramY = new ScalarInput<>(this);

        @Action
        public void compute(Integer argX, Integer argY) {
            int res = argX + argY;
            result.complete(res);
        }
    }

    @Test
    public void testAP() throws ExecutionException, InterruptedException {
        // create 3 nodes
        Square sqX = new Square();
        Square sqY = new Square();
        Sum sum = new Sum();
        // make 2 connections
        sqX.result.subscribe(sum.paramX);
        sqY.result.subscribe(sum.paramY);
        // start all the nodes
        sqX.start();
        sqY.start();
        sum.start();
        // provide input information:
        sqX.param.post(3);
        sqY.param.post(4);
        // get the result
        int res = sum.result.get();
        Assert.assertEquals(25, res);
    }

    @Test
    public void testDFF() throws ExecutionException, InterruptedException {
        Function<Integer, Integer> square = arg -> arg * arg;
        BiFunction<Integer, Integer, Integer> plus = (argX, argY) -> argX + argY;
        // create nodes and connect them
        AsyncFunction<Integer, Integer> sqX = new AsyncFunction<>(square);
        AsyncFunction<Integer, Integer> sqY = new AsyncFunction<>(square);
        AsyncBiFunction<Integer, Integer, Integer> sum = new AsyncBiFunction<Integer, Integer, Integer>(plus);
        // make 2 connections
        sqX.subscribe(sum.param1);
        sqY.subscribe(sum.param2);
        // start all the nodes
        sqX.start();
        sqY.start();
        sum.start();
        // provide input information:
        sqX.post(3);
        sqY.post(4);
        // get the result
        int res = sum.asyncResult().get();
        Assert.assertEquals(25, res);
    }

    @Test
    public void testCF() throws ExecutionException, InterruptedException {
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
        int res = sum.get();
        Assert.assertEquals(25, res);
    }

}
