package org.df4j.core.asyncproc.ext;

import org.df4j.core.asyncproc.*;
import org.df4j.core.protocols.Scalar;
import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class ActionTest {

    // smoke test
    public void computeMult(double a, double b, double expected) throws InterruptedException, ExecutionException, TimeoutException {
        AsyncBiFunction<Double, Double, Double> mult = new Mult();
        mult.param1.onSuccess(a);
        mult.param2.onSuccess(b);
        double result = mult.asyncResult().get(1, TimeUnit.SECONDS);
        Assert.assertEquals(expected, result, 0.001);
    }

    @Test
    public void runMultTest() throws InterruptedException, ExecutionException, TimeoutException {
        computeMult(3.0, 4.0, 12.0);
        computeMult(-1.0, -2.0, 2.0);
    }

    public static class Blocker<T,R> extends AsyncSupplier<R> {
        ScalarInput<T> arg = new ScalarInput<>(this);
    }

    /* D = b^2 - 4ac */
    class Discr extends AsyncSupplier<Double> {
        ScalarInput<Double> pa = new ScalarInput<>(this);
        ScalarInput<Double> pb = new ScalarInput<>(this);
        ScalarInput<Double> pc = new ScalarInput<>(this);

        @Action
        public double act(Double a, Double b, Double c) {
            return b*b - 4*a*c;
        }
    }

    private ScalarResult<Double> computeDiscr(double a, double b, double c) {
        Discr d = new Discr();
        d.pa.onSuccess(a);
        d.pb.onSuccess(b);
        d.pc.onSuccess(c);
        return d.asyncResult();
    }

    public void computeDiscrAndScheck(double a, double b, double c, double expected) throws InterruptedException, ExecutionException, TimeoutException {
        ScalarResult<Double> asyncResult = computeDiscr(a, b, c);
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
    class RootCalc extends AsyncSupplier<double[]> {
        ScalarInput<Double> pa = new ScalarInput<>(this);
        ScalarInput<Double> pb = new ScalarInput<>(this);
        ScalarInput<Double> pd = new ScalarInput<>(this);

        @Action
        public double[] act(Double a, Double b, Double d) {
            if (d < 0) {
                return new double[0];
            } else {
                double sqrt_d = Math.sqrt(d);
                double root1 = (-b - sqrt_d)/(2*a);
                double root2 = (-b + sqrt_d)/(2*a);
                return new double[]{root1, root2};
            }
        }
    }

    private ScalarResult<double[]> calcRoots(double a, double b, Scalar.Publisher<Double> d) {
        RootCalc rc = new RootCalc();
        rc.pa.onSuccess(a);
        rc.pb.onSuccess(b);
        d.subscribe(rc.pd);
        return rc.asyncResult();
    }

    public void calcRootsAndCheck(double a, double b, double d, double... expected) throws InterruptedException, ExecutionException, TimeoutException {
        ScalarResult<double[]> rc = calcRoots(a, b, ScalarResult.completedResult(d));
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
        ScalarResult<Double> d = computeDiscr(a, b, c);
        ScalarResult<double[]> rc = calcRoots(a, b, d);
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
        }

        protected Plus(Scalar.Publisher pa, Scalar.Publisher pb) {
            this();
            pa.subscribe(param1);
            pb.subscribe(param2);
        }
    }

    static class Minus extends AsyncBiFunction<Double,Double,Double> {

        public Minus() {
            super((Double val1, Double val2) -> val1 - val2);
        }

        protected Minus(Scalar.Publisher pa, Scalar.Publisher pb) {
            this();
            pa.subscribe(param1);
            pb.subscribe(param2);
        }
    }

    static class Mult extends AsyncBiFunction<Double,Double,Double> {

        public Mult() {
            super((Double val1, Double val2) -> val1 * val2);
        }

        protected Mult(Scalar.Publisher pa, Scalar.Publisher pb) {
            this();
            pa.subscribe(param1);
            pb.subscribe(param2);
        }
    }

    static class Div extends AsyncBiFunction<Double,Double,Double> {

        public Div() {
            super((Double val1, Double val2) -> val1 / val2);
        }

        protected Div(Scalar.Publisher pa, Scalar.Publisher pb) {
            this();
            pa.subscribe(param1);
            pb.subscribe(param2);
        }
    }
}
