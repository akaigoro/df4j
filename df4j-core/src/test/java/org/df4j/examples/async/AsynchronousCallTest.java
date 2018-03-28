package org.df4j.examples.async;

import org.df4j.core.AsynchronousCall;
import org.df4j.core.Port;
import org.df4j.core.ext.CompletablePromise;
import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.*;

public class AsynchronousCallTest {
    @Test
    public void runQuadraticTest() throws InterruptedException, ExecutionException, TimeoutException {
        computeQuadratic(2.0, 6.0, -8.0, -4.0, 1.0);
        computeQuadratic(3.0, 4.0, 1.0, -1.0, -0.3333);
        computeQuadratic(4.0, 2.0, 3.0);
    }

    public void computeQuadratic(double a, double b, double c, double... expectedRoots) throws InterruptedException {
        BlockingQueuePort<Double> res = new BlockingQueuePort<>();
        Quadratic equation = new Quadratic(res);
        equation.a.post(a);
        equation.b.post(b);
        equation.c.post(c);
        double root1 = res.poll(1, TimeUnit.SECONDS);
        if (expectedRoots.length == 0) {
            Assert.assertTrue(Double.isNaN(root1));
        } else {
            double root2 = res.poll(1, TimeUnit.SECONDS);
            if (root1>root2)             {
                double root = root1;
                root1 = root2;
                root2 = root;
            }
            Assert.assertEquals(expectedRoots[0], root1, 0.001);
            Assert.assertEquals(expectedRoots[1], root2, 0.001);
        }
    }

    /**
     * (-b +/- D)/2*a
     */
    static class Quadratic {
        // parameters
        CompletablePromise<Double> a = new CompletablePromise<>();
        CompletablePromise<Double> b = new CompletablePromise<>();
        CompletablePromise<Double> c = new CompletablePromise<>();

        public Quadratic(Port<Double> out){
            CompletablePromise<Double> d = new CompletablePromise<>();
            Discr discr = new Discr(d);
            a.postTo(discr.a);
            b.postTo(discr.b);
            c.postTo(discr.c);
            CompletablePromise<Double> d2 = new CompletablePromise<>();
            Condition cond = new Condition(out, d2);
            d.postTo(cond.d);
            Minus minusB = new Minus(new CompletablePromise<>());
            minusB.a.post(0.0);
            b.postTo(minusB.b);
            Mult aa = new Mult(new CompletablePromise<>());
            a.postTo(aa.a);
            aa.b.post(2.0);

            Div div1 = new Div(out);
            Plus plus = new Plus(div1.a);
            ((CompletablePromise)minusB.out).postTo(plus.a);
            d.postTo(plus.b);
            ((CompletablePromise)aa.out).postTo(div1.b);

            Div div2 = new Div(out);
            Minus minus = new Minus(div2.a);
            ((CompletablePromise)minusB.out).postTo(minus.a);
            d.postTo(minus.b);
            ((CompletablePromise)aa.out).postTo(div2.b);
        }
    }

    static class Condition extends AsynchronousCall {
        ConstInput<Double> d = new ConstInput<>();
        Port d1, d2;

        Condition(Port d1, Port d2){
            this.d1 = d1;
            this.d2 = d2;
        }

        @Override
        public void act() {
            Double d = this.d.get();
            if (Double.isNaN(d)) {
                d1.post(d);
            } else {
                d2.post(d);
            }
        }
    }

    @Test
    public void runDiscrTest() throws InterruptedException, ExecutionException, TimeoutException {
        double d = computeDiscr(3.0, 4.0, 1.0);
        Assert.assertEquals(2.0, d, 0.001);
        d = computeDiscr(3.0, 2.0, 1.0);
        Assert.assertTrue(Double.isNaN(d));
    }

    public Double computeDiscr(double a, double b, double c) throws InterruptedException, ExecutionException, TimeoutException {
        CompletablePort<Double> res = new CompletablePort<>();
        Discr discr = new Discr(res);
        discr.a.post(a);
        discr.b.post(b);
        discr.c.post(c);
        return res.get(1, TimeUnit.SECONDS);
    }

    /**
     * sqrt(b^2-4*a*c)
     */
    static class Discr  {
        // parameters
        AsynchronousCall.ConstInput<Double> a, b, c;

        Discr(Port out){
            Sqrt sqrt = new Sqrt(out);
            Minus minus = new Minus(sqrt.a);
            Square b2 = new Square(minus.a);
            b=b2.a;
            Mult m1 = new Mult(minus.b);
            c = m1.b;
            Mult m2 = new Mult(m1.a);
            m2.a.post(4.0);
            a=m2.b;
        }
    }

    static abstract class BinaryFunc<T> extends AsynchronousCall {
        ConstInput<T> a = new ConstInput<>();
        ConstInput<T> b = new ConstInput<>();
        Port out;

        BinaryFunc(){}

        BinaryFunc(Port out){
            this.out = out;
        }

        public void setOut(Port out) {
            this.out = out;
        }

        @Override
        public void act() {
            T res = compute(a.get(), b.get());
            out.post(res);
        }

        public abstract T compute(T a, T b);
    }

    static abstract class UnaryFunc<T> extends AsynchronousCall {
        ConstInput<T> a = new ConstInput<>();
        Port out;

        UnaryFunc(){}

        UnaryFunc(Port out){
            this.out = out;
        }

        public void setOut(Port out) {
            this.out = out;
        }

        @Override
        public void act() {
            T res = compute(a.get());
            out.post(res);
        }

        public abstract T compute(T a);
    }

    static class Minus extends BinaryFunc<Double> {
        Minus(){}

        Minus(Port out){
            super(out);
        }

        @Override
        public Double compute(Double a, Double b) {
            return a-b;
        }
    }

    static class Plus extends BinaryFunc<Double> {
        Plus(){}

        Plus(Port out){
            super(out);
        }

        @Override
        public Double compute(Double a, Double b) {
            return a+b;
        }
    }

    static class Sqrt extends UnaryFunc<Double> {
        Sqrt(){}

        Sqrt(Port out){
            super(out);
        }

        @Override
        public Double compute(Double a) {
            return Math.sqrt(a);
        }
    }
    static class Square extends UnaryFunc<Double> {
        Square(){}

        Square(Port out){
            super(out);
        }

        @Override
        public Double compute(Double a) {
            return a*a;
        }
    }

    static class Mult extends BinaryFunc<Double> {
        Mult(){}

        Mult(Port out){
            super(out);
        }

        @Override
        public Double compute(Double a, Double b) {
            return a*b;
        }
    }

    static class Div extends BinaryFunc<Double> {
        Div(){}

        Div(Port out){
            super(out);
        }

        @Override
        public Double compute(Double a, Double b) {
            return a/b;
        }
    }

    public static class CompletablePort<T> extends CompletableFuture<T> implements Port<T> {
        @Override
        public void post(T v) {
            super.complete(v);
        }
    }

    static class BlockingQueuePort<T> extends ArrayBlockingQueue<T> implements  Port<T> {

        public BlockingQueuePort() {
            super(4);
        }

        @Override
        public void post(T v) {
            try {
                super.put(v);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

}
