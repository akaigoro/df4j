/*
 * Copyright 2011-2014 by Alexei Kaigorodov
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.df4j.core.func;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.df4j.core.Listenable;
import org.df4j.core.func.Function;
import org.df4j.core.func.Promise;
import org.junit.Test;

/**
 * Demonstration of building functional networks.
 * Sample networks realizing well-known mathematic formulae.
 * All network nodes execute in parallel, as soon as input data are ready.
 * Nodes communicate with each other using Port and Promise/Future interfaces which have push semantics.
 * Of course this examples have no practical meaning, as overheads for message passing 
 * overwhelms all gains from parallel execution.
 * For a functional network to have a sense, node execution should perform calculations
 * large enough to exceed the message handling time (which is about 1 microsecond). 
 * 
 * @author kaigorodov
 *
 */
public class FormulaTest {
    private static final double delta = 1E-14;
    
    /**
     * computes a^2
     */
    @Test
    public void t01() throws InterruptedException, ExecutionException {
        Future<Double> square1 = new Square().setArg(2.0);
		Double res = square1.get();
        assertEquals(4, res.intValue());
        
        square1 = new Square().setArg(2.0);
        Future<Double> square2 = new Square().setArg(square1);
		res=square2.get();
        assertEquals(16, res.intValue());
    }


    /**
     * checks that execution exception is propagated to ListenableFuture
     */
    @Test
    public void t011() throws InterruptedException {
    	Future<Double> sq=new Sqrt().setArg(-2.0); // square root from negative number would cause an error
        try {
            // that error manifests itself when the result is pulled from the network
            sq.get();
            fail("no ExecutionException");
        } catch (ExecutionException e) {
            assertTrue( e.getCause() instanceof IllegalArgumentException);
        }
    }


    /**
     * checks that execution exception is propagated between actor nodes
     * @throws Exception 
     */
    @Test
    public void t012() throws Exception {
    	Future<Double> sq=new Sqrt().setArg(-2.0); // square root from negative number would cause an error 
    	Future<Double> sum=new Sum().setArgs(sq, 0);
        try {
            // that error manifests itself when the result is pulled from the network
            sum.get();
            fail("no ExecutionException");
        } catch (ExecutionException e) {
            assertTrue( e.getCause() instanceof IllegalArgumentException);
        }
    }

    /**
     * computes 2*3
     * @throws Exception 
     */
    @Test
    public void t02() throws Exception {
    	Future<Double> node=new Mult().setArgs(2.0, 3.0);
        assertEquals(6, node.get().intValue());
    }

    /**
     * Complex network construction.
     * computes module of vector (a,b): sqrt(a^2+b^2)
     * @throws Exception 
     */
    @Test
    public void t03() throws Exception {
        // create nodes
    	Square a=new Square();  
    	Square b=new Square();  
    	Future<Double> sum = new Sum().setArgs(a, b); // a^2+b^2 -> sum
    	Future<Double> sq=new Sqrt().setArg(sum); // sum -> sqrt 
        a.setArg(3.0);
        b.setArg(4.0);
        // wait for the result
        double res = sq.get();
        assertEquals(5, res, delta);
    }


    /**
     * Demonstrates how complex network with single result
     * can be encapsulated in a class which extends {@link Promise}.
     * 
     * computes the discriminant of a quadratic equation
     *     D= b^2-4*a*c 
     */
    public static class Discr extends Function<Double> {
		@Override
		protected Object eval(Object[] args) {
			Double a = (Double) args[0];
			Double b = (Double) args[1];
			Double c = (Double) args[2];
            return new Diff().setArgs(
                    new Mult().setArgs(b, b),
                    new Mult().setArgs(4.0, new Mult().setArgs(a, c))
                );
		}
	}
	
    /**
     * Demonstrates how complex network with 2 results
     * can be encapsulated in a class with 2 {@link Listenable} members.
     * 
     * compute roots of a quadratic equation
     *     d  = b^2-4*a*c 
     *     sd = sqrt(D)
     *     a2=2*a
     *     x1 = (-b + sd)/a2
     *     x2 = (-b - sd)/a2
     */
	static class QuadEq {
        // inputs
	    Promise<Double> a=new Promise<Double>(); // a and b used multiple times, require Promise
	    Promise<Double> b=new Promise<Double>();
	    Promise<Double> c=new Promise<Double>();
        // internal nodes
		Future<Double> d=new Discr().setArgs(a, b, c);
		Future<Double> sd=new Sqrt().setArgs(d);
		Future<Double> a2=new Mult().setArgs(2.0, a);
		Future<Double> mb=new UnaryMinus().setArg(b);
		// results
		Future<Double> x1=new Div().setArgs(
				  new Sum().setArgs(mb, sd),
				  a2
				);
		Future<Double> x2=new Div().setArgs(
				  new Diff().setArgs(mb, sd),
				  a2
				);
	}

	/** checks evaluation of quadratic equation
	 */
    @Test
    public void t04() throws InterruptedException, ExecutionException {
        QuadEq node = new QuadEq();
        node.a.post(2.0);
        node.b.post(3.0);
        node.c.post(-14.0);

        assertEquals(2.0, node.x1.get(), delta);
        assertEquals(-3.5, node.x2.get(), delta);
    }

    /**
     * checks that execution exceptions are propagated
     */
    @Test
    public void t041() throws InterruptedException, ExecutionException {
        QuadEq node = new QuadEq();
        node.a.post(2.0);
        node.b.post(3.0);
        node.c.post(14.0);

        try {
            node.x1.get().intValue();
            fail("no ExecutionException");
        } catch (ExecutionException e) {
            assertTrue( e.getCause() instanceof IllegalArgumentException);
        }
        try {
            node.x2.get().intValue();
            fail("no ExecutionException");
        } catch (ExecutionException e) {
            assertTrue( e.getCause() instanceof IllegalArgumentException);
        }
    }

    // functional computing nodes

    /**
     * Unary operation
     *
     * @param <T> type of the operand and the result
     */
 	public static abstract class UnaryOp<T> extends Function<T> {

 		public Function<T> setArg(Object value) {
 			return setArgs(new Object[] { value });
 		}

 		@Override
 		@SuppressWarnings("unchecked")
 		protected T eval(Object[] args) {
 			return eval((T) args[0]);
 		}

 		abstract protected T eval(T operand);

 	}
    
    /**
     * Binary operation: classic dataflow object.
     * Waits for both operands to arrive,
     * computes the operation, and sends result to the listeners
     *
     * @param <T> the type of operands and the result
     */
    public static abstract class BinaryOp<T> extends Function<T> {

		@Override
		@SuppressWarnings("unchecked")
		protected T eval(Object[] args) {
			return eval((T) args[0], (T) args[1]);
		}

       abstract protected T eval(T opnd, T opnd2);

    }

     static class Sqrt extends UnaryOp<Double> {
    	 
        public Double eval(Double v) {
            double val = Math.sqrt(v.doubleValue());
            if (Double.isNaN(val)) {
                throw new IllegalArgumentException();
            }
            return val;
        }
    }

    static class Square extends UnaryOp<Double> {
        public Double eval(Double v) {
            return v * v;
        }
    }

    static class Sum extends BinaryOp<Double> {
        public Double eval(Double v1, Double v2) {
            return v1 + v2;
        }
    }

    static class Mult extends BinaryOp<Double> {
        public Double eval(Double v1, Double v2) {
            return v1 * v2;
        }
    }

    static class UnaryMinus extends UnaryOp<Double> {
        public Double eval(Double v) {
            return -v;
        }
    }

    static class Diff extends BinaryOp<Double> {
        public Double eval(Double v1, Double v2) {
            return v1 - v2;
        }
    }

    static class Div extends BinaryOp<Double> {
        public Double eval(Double v1, Double v2) {
            return v1 / v2;
        }
    }

    public static void main(String args[]) throws InterruptedException, ExecutionException {
    	FormulaTest qt = new FormulaTest();
        qt.t01();
    }
}
