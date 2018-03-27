package org.df4j.examples.async;

import org.df4j.core.AsynchronousCall;
import org.df4j.core.Port;
import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class AcynchronousCallTest {
    @Test
    public void runQuadraticTest() throws InterruptedException {
    }

    static class Quadratic {
        public Quadratic(double a, double b, double c) {

        }
    }

    @Test
    public void runDiscrTest() throws InterruptedException, ExecutionException {
        CompletableFuture<Double> res = new CompletableFuture<>();
        Port<Double> constInput = new Port<Double>() {
            @Override
            public void post(Double v) {
                res.complete(v);
            }
        };
        Discr discr = new Discr(constInput);
        discr.a.post(3.0);
        discr.b.post(4.0);
        discr.c.post(1.0);
        double v = res.get();
        Assert.assertEquals(2.0, v, 0.001);
    }

    /**
     * sqrt(b^2-4*a*c)
     */
    static class Discr extends CompletableFuture<Double> {
        // parameters
        AsynchronousCall.ConstInput<Double> a, b, c;
        Port out;

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
}
