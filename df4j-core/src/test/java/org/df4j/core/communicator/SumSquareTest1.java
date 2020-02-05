package org.df4j.core.communicator;

import org.df4j.core.dataflow.AsyncFunc;
import org.df4j.core.port.InpScalar;
import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BiFunction;
import java.util.function.Function;

import static org.junit.Assert.fail;

public class SumSquareTest1 {

    public static class Square extends AsyncFunc<Integer> {
        final InpScalar<Integer> param = new InpScalar<>(this);
        {
            this.start();}

        protected Integer callAction() {
            Integer arg = param.current();
            int res = arg*arg;
            return res;
        }
    }

    public static class Sum extends AsyncFunc<Integer> {
        final InpScalar<Integer> paramX = new InpScalar<>(this);
        final InpScalar<Integer> paramY = new InpScalar<>(this);
        {
            this.start();}

        protected Integer callAction() {
            Integer argX = paramX.current();
            Integer argY = paramY.current();
            int res = argX + argY;
            return res;
        }
    }

    /*
     * computes arithmetic expression sum = 3*3 + 4*4
     */
    @Test
    public void testAP() throws TimeoutException {
        // create 3 nodes
        Square sqX = new Square();
        Square sqY = new Square();
        Sum sum = new Sum();
        // connect them
        sqX.subscribe(sum.paramX);
        sqY.subscribe(sum.paramY);
        // provide input information:
        sqX.param.onSuccess(3);
        sqY.param.onSuccess(4);
        // get the result
        boolean finished = sum.blockingAwait(1, TimeUnit.SECONDS);
        if (!finished) {
            fail("not finished in time");
        }
        Integer res = sum.get(0, TimeUnit.SECONDS);
        Assert.assertNotNull(res);
        Assert.assertEquals(25, res.intValue());
    }

    /*
     * the same algorithm as in {@link #testAP()} but implemented using {@link CompletableFuture}.
     */
    @Test
    public void testCF() throws ExecutionException, InterruptedException, TimeoutException {
        Function<Integer, Integer> square = arg -> arg * arg;
        BiFunction<Integer, Integer, Integer> plus = (argX, argY) -> argX + argY;
        // create nodes and connect them
        CompletableFuture<Integer> sqXParam = new CompletableFuture();
        CompletableFuture<Integer> sqYParam = new CompletableFuture();
        CompletableFuture<Integer> sum = sqXParam.thenApply(square)
                .thenCombine(sqYParam.thenApply(square), plus);
        // provide input information:
        sqXParam.complete(3);
        sqYParam.complete(4);
        // get the result
        int res = sum.get(1, TimeUnit.SECONDS);
        Assert.assertEquals(25, res);
    }
}
