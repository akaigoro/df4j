package org.df4j.core.dataflow;

import org.df4j.core.port.InpScalar;
import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Illustration how to declare non-repeatable asynchronous procedures
 */
public class SumSquareTest {

    public static class Square extends AsyncFunc<Integer> {
        InpScalar<Integer> param = new InpScalar<>(this);

        @Override
        protected Integer callAction() throws Throwable {
            Integer arg = param.current();
            int res = arg*arg;
            return res;
        }
    }

    public static class Sum extends AsyncFunc<Integer> {
        InpScalar<Integer> paramX = new InpScalar<>(this);
        InpScalar<Integer> paramY = new InpScalar<>(this);

        @Override
        protected Integer callAction() throws Throwable {
            Integer argX = paramX.current();
            Integer argY = paramY.current();
            int res = argX + argY;
            return res;
        }
    }

    /*
     * computes arithmetic expression sum = 3*3 + 4*4 using {@link AsyncProc}edures
     * each node of dataflow graph is declared and created explicitely
     */
    @Test
    public void testAcyncProc() throws TimeoutException, InterruptedException {
        // create 3 nodes
        Square sqX = new Square();
        Square sqY = new Square();
        Sum sum = new Sum();
        // make 2 connections
        sqX.subscribe(sum.paramX);
        sqY.subscribe(sum.paramY);
        // provide input information:
        sqX.param.onSuccess(3);
        sqY.param.onSuccess(4);
        sqX.start();
        sqY.start();
        sum.start();
        // get the result
        boolean fin = sum.blockingAwait(1, TimeUnit.SECONDS);
        Assert.assertTrue(fin);
        int res = sum.get();
        Assert.assertEquals(25, res);
    }

    /*
     * the same computation made by {@link CompletableFuture}s
     * nodes of the dataflow graph are created implicitly
     */
    @Test
    public void testCompletableFuture() throws ExecutionException, InterruptedException, TimeoutException {
        // parameters are declared as separate nodes
        CompletableFuture<Integer> sqXParam = new CompletableFuture();
        CompletableFuture<Integer> sqYParam = new CompletableFuture();
        // nodes are constricted by factory methods
        // creation of connections is mixed with creation of nodes
        CompletableFuture<Integer> sum = sqXParam
                .thenApply(arg -> arg * arg)
                .thenCombine(sqYParam.thenApply(arg -> arg * arg),
                        (argX, argY) -> argX + argY);
        // provide input information:
        sqXParam.complete(3);
        sqYParam.complete(4);
        // get the result
        int res = sum.get(1, TimeUnit.SECONDS);
        Assert.assertEquals(25, res);
    }
}
