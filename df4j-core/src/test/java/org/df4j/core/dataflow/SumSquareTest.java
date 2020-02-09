package org.df4j.core.dataflow;

import org.df4j.core.communicator.ScalarResult;
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

    public static class Square extends AsyncProc {
        InpScalar<Integer> param = new InpScalar<>(this);
        ScalarResult<Integer> result = new ScalarResult<>();

        public void runAction() {
            Integer arg = param.current();
            int res = arg*arg;
            result.onSuccess(res);
        }
    }

    public static class Sum extends AsyncProc {
        InpScalar<Integer> paramX = new InpScalar<>(this);
        InpScalar<Integer> paramY = new InpScalar<>(this);
        ScalarResult<Integer> result = new ScalarResult<>();

        public void runAction() {
            Integer argX = paramX.current();
            Integer argY = paramY.current();
            int res = argX + argY;
            result.onSuccess(res);
        }
    }

    /*
     * computes arithmetic expression sum = 3*3 + 4*4 using {@link AsyncProc}edures
     * each node of dataflow graph is declared and created explicitely
     */
    @Test
    public void testAcyncProc() throws ExecutionException, InterruptedException, TimeoutException {
        // create 3 nodes
        Square sqX = new Square();
        Square sqY = new Square();
        Sum sum = new Sum();
        // make 2 connections
        sqX.result.subscribe(sum.paramX);
        sqY.result.subscribe(sum.paramY);
        // provide input information:
        sqX.param.onSuccess(3);
        sqY.param.onSuccess(4);
        sqX.start();
        sqY.start();
        sum.start();
        // get the result
        int res = sum.result.get(1, TimeUnit.SECONDS);
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
