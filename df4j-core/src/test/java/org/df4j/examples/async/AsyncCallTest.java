package org.df4j.examples.async;

import org.df4j.core.AsynchronousCall;
import org.junit.Test;

public class AsyncCallTest {
    @Test
    public void runQuadraticTest() throws InterruptedException {
    }

    static class Quadratic {
        public Quadratic(double a, double b, double c) {

        }
    }

    /**
     * sqrt(b^2-4*a*c)
     */
    static class Discr {
        UnaryFunc<Double> res = new UnaryFunc<Double>() {

            @Override
            public Double compute(Double a) {
                return Math.sqrt(a);
            }
        };
       // b2 = new Square();
    }

    static abstract class BinaryFunc<T> extends AsynchronousCall {
        ConstInput<T> a = new ConstInput<>();
        ConstInput<T> b = new ConstInput<>();
        Input out;

        public void setOut(Input out) {
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
        Input out;

        public void setOut(Input out) {
            this.out = out;
        }

        @Override
        public void act() {
            T res = compute(a.get());
            out.post(res);
        }

        public abstract T compute(T a);
    }

    static class Plus extends BinaryFunc<Double> {
        @Override
        public Double compute(Double a, Double b) {
            return a+b;
        }
    }

    static class Minus extends BinaryFunc<Double> {
        @Override
        public Double compute(Double a, Double b) {
            return a-b;
        }
    }

    static class Square extends UnaryFunc<Double> {
        @Override
        public Double compute(Double a) {
            return a*a;
        }
    }

    static class Mult extends BinaryFunc<Double> {
        @Override
        public Double compute(Double a, Double b) {
            return a*b;
        }
    }

    static class Div extends BinaryFunc<Double> {
        @Override
        public Double compute(Double a, Double b) {
            return a/b;
        }
    }
}
