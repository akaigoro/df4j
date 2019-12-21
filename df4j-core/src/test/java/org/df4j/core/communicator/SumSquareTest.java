package org.df4j.core.communicator;

import org.df4j.core.actor.AsyncProc;
import org.df4j.core.port.ScalarInput;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.util.concurrent.*;
import java.util.function.BiFunction;
import java.util.function.Function;

import static org.junit.Assert.fail;

public class SumSquareTest {

    public static class Square extends AsyncProc {
        final ScalarInput<Integer> param = new ScalarInput<>(this);
        final ScalarResult<Integer> out = new ScalarResult<>();
        {awake();}

        protected void runAction() {
            Integer arg = param.current();
            int res = arg*arg;
            out.onSuccess(res);
        }
    }

    public static class Sum extends AsyncProc {
        final ScalarInput<Integer> paramX = new ScalarInput<>(this);
        final ScalarInput<Integer> paramY = new ScalarInput<>(this);
        final ScalarResult<Integer> out = new ScalarResult<>();
        {awake();}

        protected void runAction() {
            Integer argX = paramX.current();
            Integer argY = paramY.current();
            int res = argX + argY;
            out.onSuccess(res);
        }
    }

    /**
     * computes arithmetic expression sum = 3*3 + 4*4
     */
    @Test
    public void testAP() throws InterruptedException, ExecutionException, TimeoutException {
        // create 3 nodes
        Square sqX = new Square();
        Square sqY = new Square();
        Sum sum = new Sum();
        // connect them
        sqX.out.subscribe(sum.paramX);
        sqY.out.subscribe(sum.paramY);
        // provide input information:
        sqX.param.onSuccess(3);
        sqY.param.onSuccess(4);
        // get the result
        boolean finished = sum.blockingAwait(1, TimeUnit.SECONDS);
        if (!finished) {
            fail("not finished in time");
        }
        Integer res = sum.out.get(0, TimeUnit.SECONDS);
        Assert.assertNotNull(res);
        Assert.assertEquals(25, res.intValue());
    }

    /**
     * the same algorithm as in {@link #testAP()} but implemented using {@link CompletableFuture}.
     *
     * @throws ExecutionException
     * @throws InterruptedException
     * @throws TimeoutException
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
