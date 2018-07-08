package org.df4j.core.messagescalar;

import org.df4j.core.connector.messagescalar.*;
import org.df4j.core.node.Action;
import org.df4j.core.node.AsyncActionTask;
import org.df4j.core.node.messagescalar.AsyncBiFunction;
import org.df4j.core.node.AsyncResult;
import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class AsyncTaskTest {

    // smoke test
    public void computeMult(double a, double b, double expected) throws InterruptedException, ExecutionException, TimeoutException {
        AsyncBiFunction<Double, Double, Double> mult = new Mult();
        mult.arg1.post(a);
        mult.arg2.post(b);
        double result = mult.asyncResult().get(1, TimeUnit.SECONDS);
        Assert.assertEquals(expected, result, 0.001);
    }

    @Test
    public void runMultTest() throws InterruptedException, ExecutionException, TimeoutException {
        computeMult(3.0, 4.0, 12.0);
        computeMult(-1.0, -2.0, 2.0);
    }

    public static class Blocker<T,R> extends AsyncResult<R> {
        ConstInput<T> arg = new ConstInput<>(this);
    }

    class Mult2 extends Mult {
        CompletablePromise<Double> pa = new CompletablePromise<>();
        CompletablePromise<Double> pb = new CompletablePromise<>();

        protected Mult2() {
            CompletablePromise<Double> sp = new CompletablePromise<>();
            Blocker<Double, Double> blocker = new Blocker<>();
            pa.subscribe(blocker.arg);
            new CompletedPromise<Double>(1.0).subscribe(arg1);
            new Mult(pa, pb).subscribe(arg2);
        }
    }

    public void computeMult2(double a, double b, double expected) throws InterruptedException, ExecutionException, TimeoutException {
        Mult2 mult = new Mult2();
        mult.pa.complete(a);
        mult.pb.complete(b);
        double result = mult.asFuture().get(1, TimeUnit.SECONDS);
        Assert.assertEquals(expected, result, 0.001);
    }

    @Test
    public void runMultTest2() throws InterruptedException, ExecutionException, TimeoutException {
        computeMult2(3.0, 4.0, 12.0);
        computeMult2(-1.0, -2.0, 2.0);
    }

    /* D = b^2 - 4ac */
    class Discr extends AsyncActionTask<Double> {
        ConstInput<Double> pa = new ConstInput<>(this);
        ConstInput<Double> pb = new ConstInput<>(this);
        ConstInput<Double> pc = new ConstInput<>(this);

        @Action
        public void act(Double a, Double b, Double c) {
            double d = b*b -4*a*c;
            complete(d);
        }
    }

    private CompletablePromise<Double> computeDiscr(double a, double b, double c) {
        Discr d = new Discr();
        d.pa.post(a);
        d.pb.post(b);
        d.pc.post(c);
        d.start();
        return d.asyncResult();
    }

    public void computeDiscrAndScheck(double a, double b, double c, double expected) throws InterruptedException, ExecutionException, TimeoutException {
        CompletablePromise<Double> asyncResult = computeDiscr(a, b, c);
        Double result = asyncResult.get(1, TimeUnit.SECONDS);
        Assert.assertEquals(expected, result, 0.001);
    }

    @Test
    public void runDiscrTest() throws InterruptedException, ExecutionException, TimeoutException {
        computeDiscrAndScheck(3.0, -4.0, 1.0, 4.0);
        computeDiscrAndScheck(1.0, 4.0, 4.0, 0.0);
        computeDiscrAndScheck(2.0, 6.0, 5.0, -4.0);
    }

    /**
     * (-b +/- sqrt(D))/2a
     */
    class RootCalc extends AsyncActionTask<double[]> {
        ConstInput<Double> pa = new ConstInput<>(this);
        ConstInput<Double> pb = new ConstInput<>(this);
        ConstInput<Double> pd = new ConstInput<>(this);

        @Action
        public void act(Double a, Double b, Double d) {
            double[] res;
            if (d < 0) {
                res = new double[0];
            } else {
                double sqrt_d = Math.sqrt(d);
                double root1 = (-b - sqrt_d)/(2*a);
                double root2 = (-b + sqrt_d)/(2*a);
                res = new double[]{root1, root2};
            }
            this.result.complete(res);
        }
    }

    private CompletablePromise<double[]> calcRoots(double a, double b, ScalarPublisher<Double> d) {
        RootCalc rc = new RootCalc();
        rc.pa.post(a);
        rc.pb.post(b);
        d.subscribe(rc.pd);
        rc.start();
        return rc.asyncResult();
    }

    public void calcRootsAndCheck(double a, double b, double d, double... expected) throws InterruptedException, ExecutionException, TimeoutException {
        CompletablePromise<double[]> rc = calcRoots(a, b, new CompletedPromise(d));
        double[] result = rc.get(1, TimeUnit.SECONDS);
        Assert.assertArrayEquals(expected, result, 0.001);
    }

    @Test
    public void calcRootsTest() throws InterruptedException, ExecutionException, TimeoutException {
        calcRootsAndCheck(1.0, -4.0, 4.0, 1.0, 3.0);
        calcRootsAndCheck(1.0, 4.0, 0.0, -2.0, -2.0);
        calcRootsAndCheck(1.0, 6.0, -4.0);
    }

    public void computeRoots(double a, double b, double c, double... expected) throws InterruptedException, ExecutionException, TimeoutException {
        CompletablePromise<Double> d = computeDiscr(a, b, c);
        CompletablePromise<double[]> rc = calcRoots(a, b, d);
        double[] result = rc.get(1, TimeUnit.SECONDS);
        Assert.assertArrayEquals(expected, result, 0.001);
    }

    @Test
    public void equationTest() throws InterruptedException, ExecutionException, TimeoutException {
        computeRoots(3.0, -4.0, 1.0, 0.333, 1.0);
        computeRoots(1.0, 4.0, 4.0, -2.0, -2.0);
        computeRoots(1.0, 6.0, 45.0);
    }

    static class Plus extends AsyncBiFunction<Double,Double,Double> {

        public Plus() {
            super((Double val1, Double val2) -> val1 + val2);
            start();
        }

        protected Plus(ScalarPublisher pa, ScalarPublisher pb) {
            this();
            pa.subscribe(arg1);
            pb.subscribe(arg2);
        }
    }

    static class Minus extends AsyncBiFunction<Double,Double,Double> {

        public Minus() {
            super((Double val1, Double val2) -> val1 - val2);
            start();
        }

        protected Minus(ScalarPublisher pa, ScalarPublisher pb) {
            this();
            pa.subscribe(arg1);
            pb.subscribe(arg2);
        }
    }

    static class Mult extends AsyncBiFunction<Double,Double,Double> {

        public Mult() {
            super((Double val1, Double val2) -> val1 * val2);
            start();
        }

        protected Mult(ScalarPublisher pa, ScalarPublisher pb) {
            this();
            pa.subscribe(arg1);
            pb.subscribe(arg2);
        }
    }

    static class Div extends AsyncBiFunction<Double,Double,Double> {

        public Div() {
            super((Double val1, Double val2) -> val1 / val2);
            start();
        }

        protected Div(ScalarPublisher pa, ScalarPublisher pb) {
            this();
            pa.subscribe(arg1);
            pb.subscribe(arg2);
        }
    }
}
