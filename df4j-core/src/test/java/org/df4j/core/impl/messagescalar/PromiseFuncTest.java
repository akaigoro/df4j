package org.df4j.core.impl.messagescalar;

import org.df4j.core.impl.messagescalar.promisefuncs.*;
import org.df4j.core.spi.messagescalar.Promise;
import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * this is how the df4j library is supposed to be used:
 *  a) user creats his own library for his problem domain - here, package promisefuncs
 *  b) using this library and df4j itself, computational graph created (here class {@link QuadraticRoots}
 *    if this class is big, parts of the graph are described as separate classes (here {@link SortedRoots} and  {@link SortedRoots} )
 *  c) the graph is run with different arguments.
 */
public class PromiseFuncTest {
    @Test
    public void runQuadraticTest() throws InterruptedException, ExecutionException, TimeoutException {
        computeQuadratic(3.0, 4.0, 1.0, -1.0, -0.3333);
        computeQuadratic(2.0, 6.0, -8.0, -4.0, 1.0);
        computeQuadratic(4.0, 2.0, 3.0);
    }

    public void computeQuadratic(double a, double b, double c, double... expectedRoots) throws InterruptedException, TimeoutException, ExecutionException {
        QuadraticRoots equation = new QuadraticRoots(new CompletedPromise<>(a), new CompletedPromise<>(b), new CompletedPromise<>(c));

        double[] roots = equation.asFuture().get(1, TimeUnit.SECONDS);
        Assert.assertEquals(expectedRoots.length, roots.length);
        if (expectedRoots.length == 2) {
            Assert.assertEquals(expectedRoots[0], roots[0], 0.001);
            Assert.assertEquals(expectedRoots[1], roots[1], 0.001);
        }
    }

    /**
     * D = sqrt(b^2-4*a*c)
     * roots[0] = (-b + D)/2*a
     * roots[1] = (-b - D)/2*a
     */
    static class QuadraticRoots extends PromiseFunc<double[]> {
        Promise<Double> pa, pb;
        ConstInput<Double> discr=new ConstInput<Double>();

        QuadraticRoots(Promise<Double> pa, Promise<Double> pb, Promise<Double> pc) {
            this.pa = pa;
            this.pb = pb;
            new Discr(pa, pb, pc).postTo(discr);
        }

        @Override
        public void act() {
            Double d = discr.get();
            if (d <0) {
                out.post(new double[0]);
            } else {
                Promise<Double> pd = new Sqrt(new CompletedPromise<>(d));
                Promise<Double> mb = new Minus(new CompletedPromise<>(0.0), pb);
                Promise<Double> a2 = new Mult(new CompletedPromise<>(2.0), pa);
                new SortedRoots(
                        new Div(new Plus(mb, pd), a2),
                        new Div(new Minus(mb, pd), a2)
                ).postTo(out);
            }
        }
    }

    static class SortedRoots extends PromiseFunc<double[]> {
        ConstInput<Double> root1 = new ConstInput<Double>();
        ConstInput<Double> root2 = new ConstInput<Double>();

        SortedRoots(Promise<Double> root1, Promise<Double> root2) {
            root1.postTo(this.root1);
            root2.postTo(this.root2);
        }

        @Override
        protected void act() throws Exception {
            double value1 = root1.get();
            double value2 = root2.get();
            if (value1>value2) {
                double value = value1; value1 = value2; value2 = value;
            }
            out.post(new double[]{value1, value2});
        }
    }

    @Test
    public void runDiscrTest() throws InterruptedException, ExecutionException, TimeoutException {
        computeDiscr(3.0, 4.0, 1.0, 4.0);
        computeDiscr(3.0, 2.0, 1.0, -8.0);
    }

    public void computeDiscr(double a, double b, double c, double expected) throws InterruptedException, ExecutionException, TimeoutException {
        Promise<Double> res = new Discr(new CompletedPromise<>(a), new CompletedPromise<>(b), new CompletedPromise<>(c));
        double result = res.asFuture().get(1, TimeUnit.SECONDS);
        Assert.assertEquals(expected, result, 0.001);
    }

    static class Discr extends Minus {

        Discr(Promise<Double> pa, Promise<Double> pb, Promise<Double> pc) {
            super(new Square(pb),
                  new Mult(new CompletedPromise<>(4.0),
                          new Mult(pa, pc)
                  )
            );
        }
    }

    @Test
    public void runMultTest() throws InterruptedException, ExecutionException, TimeoutException {
        computeMult(3.0, 4.0, 12.0);
        computeMult(-1.0, -2.0, 2.0);
    }

    public void computeMult(double a, double b, double expected) throws InterruptedException, ExecutionException, TimeoutException {
        Mult mult = new Mult(new CompletedPromise<>(a), new CompletedPromise<>(b));
        double result = mult.asFuture().get(1, TimeUnit.SECONDS);
        Assert.assertEquals(expected, result, 0.001);
    }

    static class Mult extends BinaryPromiseFunc<Double,Double,Double> {
        Mult(Promise<Double> pa, Promise<Double> pb) {
            super((Double a,Double b)-> a*b, pa, pb);
        }
    }

    static class Minus extends BinaryPromiseFunc<Double,Double,Double> {
        Minus(Promise<Double> pa, Promise<Double> pb) {
            super((a,b)-> a-b, pa, pb);
        }
    }

    static class Plus extends BinaryPromiseFunc<Double,Double,Double> {
        Plus(Promise<Double> pa, Promise<Double> pb) {
            super((a,b)-> a+b, pa, pb);
        }
    }

    static class Sqrt extends UnaryPromiseFunc<Double,Double> {
        Sqrt(Promise<Double> pa) {
            super((a)->Math.sqrt(a), pa);
        }
    }

    static class Square extends UnaryPromiseFunc<Double,Double> {
        Square(Promise<Double> pa) {
            super((a)->a*a, pa);
        }
    }

    static class Div extends BinaryPromiseFunc<Double,Double,Double> {
        Div(Promise<Double> pa, Promise<Double> pb) {
            super((a,b)-> a/b, pa, pb);
        }
    }
}
