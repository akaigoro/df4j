package org.df4j.core.messagescalar;

import org.df4j.core.connector.messagescalar.ConstInput;
import org.df4j.core.connector.messagescalar.ScalarPublisher;
import org.df4j.core.node.Action;
import org.df4j.core.node.messagescalar.CompletedPromise;
import org.df4j.core.node.messagescalar.SimplePromise;
import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class AsyncTaskTest {

    // smoke test
    public void computeMult(double a, double b, double expected) throws InterruptedException, ExecutionException, TimeoutException {
        AsyncFunction.BinaryAsyncFunction<Double, Double, Double> mult = new Mult();
        mult.arg1.post(a);
        mult.arg2.post(b);
        double result = mult.asFuture().get(1, TimeUnit.SECONDS);
        Assert.assertEquals(expected, result, 0.001);
    }

    @Test
    public void runMultTest() throws InterruptedException, ExecutionException, TimeoutException {
        computeMult(3.0, 4.0, 12.0);
        computeMult(-1.0, -2.0, 2.0);
    }

    public static class Blocker<T,R> extends AsyncFunction<R> {
        ConstInput<T> arg = new ConstInput<>(this);
    }

    class Mult2 extends Mult {
        SimplePromise<Double> pa = new SimplePromise<>();
        SimplePromise<Double> pb = new SimplePromise<>();

        protected Mult2() {
            SimplePromise<Double> sp = new SimplePromise<>();
            Blocker<Double, Double> blocker = new Blocker<>();
            pa.subscribe(blocker.arg);
            new CompletedPromise<Double>(1.0).subscribe(arg1);
            new Mult(pa, pb).subscribe(arg2);
        }
    }

    public void computeMult2(double a, double b, double expected) throws InterruptedException, ExecutionException, TimeoutException {
        Mult2 mult = new Mult2();
        mult.pa.post(a);
        mult.pb.post(b);
        double result = mult.asFuture().get(1, TimeUnit.SECONDS);
        Assert.assertEquals(expected, result, 0.001);
    }

    @Test
    public void runMultTest2() throws InterruptedException, ExecutionException, TimeoutException {
        computeMult2(3.0, 4.0, 12.0);
        computeMult2(-1.0, -2.0, 2.0);
    }

    /* D = b^2 - 4ac */
    class Discr extends Minus {
        SimplePromise<Double> pa = new SimplePromise<>();
        SimplePromise<Double> pb = new SimplePromise<>();
        SimplePromise<Double> pc = new SimplePromise<>();

        protected Discr() {
            new Mult(pb, pb).subscribe(arg1);
            new Mult(
                    new CompletedPromise<Double>(4.0),
                    new Mult(pa, pc)
            ).subscribe(arg2);
        }
    }

    public void computeDiscr(double a, double b, double c, double expected) throws InterruptedException, ExecutionException, TimeoutException {
        Discr d = new Discr();
        d.pa.post(a);
        d.pb.post(b);
        d.pc.post(c);
        Double result = d.asFuture().get(1, TimeUnit.SECONDS);
        Assert.assertEquals(expected, result, 0.001);
    }

    @Test
    public void runDiscrTest() throws InterruptedException, ExecutionException, TimeoutException {
        computeDiscr(3.0, -4.0, 1.0, 4.0);
        computeDiscr(1.0, 4.0, 4.0, 0.0);
        computeDiscr(2.0, 6.0, 5.0, -4.0);
    }

    /**
     * (-b +/- sqrt(D))/2a
     */
    class RootCalc extends AsyncFunction<double[]> {
        SimplePromise<Double> pa = new SimplePromise<>();
        SimplePromise<Double> pb = new SimplePromise<>();
        ConstInput<Double> pd = new ConstInput<>(this);

        @Action
        public void act(Double d) {
            ScalarPublisher<Double> sqrt_d;
            if (d < 0) {
                result.post(new double[0]);
                return;
            } else {
                sqrt_d = new CompletedPromise<>(Math.sqrt(d));
            }
            ScalarPublisher<Double> minus_b = new Minus(new CompletedPromise<Double>(0.0), pb);
            ScalarPublisher<Double> a_twice = new Mult(new CompletedPromise<Double>(2.0), pa);
            BinaryAsyncFunction<Double, Double, double[]> calcRoots = new BinaryAsyncFunction<Double, Double, double[]>(
                    new Div(new Minus(minus_b, sqrt_d), a_twice),
                    new Div(new Plus(minus_b, sqrt_d), a_twice)
            ) {
                @Override
                protected double[] apply(Double val1, Double val2) {
                    return new double[]{val1.doubleValue(), val2.doubleValue()};
                }
            };
            calcRoots.subscribe(result);
        }
    }

    public void calcRoots(double a, double b, double d, double... expected) throws InterruptedException, ExecutionException, TimeoutException {
        RootCalc rc = new RootCalc();
        rc.pa.post(a);
        rc.pb.post(b);
        rc.pd.post(d);
        rc.start();

        double[] result = rc.asFuture().get(1, TimeUnit.SECONDS);
        Assert.assertArrayEquals(expected, result, 0.001);
    }

    @Test
    public void calcRootsTest() throws InterruptedException, ExecutionException, TimeoutException {
        calcRoots(1.0, -4.0, 4.0, 1.0, 3.0);
        calcRoots(1.0, 4.0, 0.0, -2.0, -2.0);
        calcRoots(1.0, 6.0, -4.0);
    }

    class Equation extends RootCalc {
        SimplePromise<Double> pc = new SimplePromise<>();
        Discr d = new Discr();
        {
            pa.subscribe(d.pa);
            pb.subscribe(d.pb);
            pc.subscribe(d.pc);
            d.subscribe(pd);
        }
    }

    public void computeRoots(double a, double b, double c, double... expected) throws InterruptedException, ExecutionException, TimeoutException {
        Equation equ = new Equation();
        equ.pa.post(a);
        equ.pb.post(b);
        equ.pc.post(c);
        equ.start();

        double[] result = equ.asFuture().get(2, TimeUnit.SECONDS);
        Assert.assertArrayEquals(expected, result, 0.001);
    }

    @Test
    public void equationTest() throws InterruptedException, ExecutionException, TimeoutException {
        computeRoots(3.0, -4.0, 1.0, 0.333, 1.0);
        computeRoots(1.0, 4.0, 4.0, -2.0, -2.0);
        computeRoots(1.0, 6.0, 45.0);
    }

    static class Plus extends AsyncFunction.BinaryAsyncFunction<Double,Double,Double> {
        protected Plus(ScalarPublisher pa, ScalarPublisher pb) {
            super(pa, pb);
        }

        public Plus() { }

        @Override
        protected Double apply(Double val1, Double val2) {
            return val1 + val2;
        }
    }

    static class Minus extends AsyncFunction.BinaryAsyncFunction<Double,Double,Double> {
        protected Minus(ScalarPublisher pa, ScalarPublisher pb) {
            super(pa, pb);
        }

        public Minus() { }

        @Override
        protected Double apply(Double val1, Double val2) {
            return val1 - val2;
        }
    }

    static class Mult extends AsyncFunction.BinaryAsyncFunction<Double,Double,Double> {
        protected Mult(ScalarPublisher pa, ScalarPublisher pb) {
            super(pa, pb);
        }

        public Mult() {
        }

        @Override
        protected Double apply(Double val1, Double val2) {
            return val1 * val2;
        }
    }

    static class Div extends AsyncFunction.BinaryAsyncFunction<Double,Double,Double> {
        protected Div(ScalarPublisher pa, ScalarPublisher pb) {
            super(pa, pb);
        }

        @Override
        protected Double apply(Double val1, Double val2) {
            return val1 / val2;
        }
    }
}
