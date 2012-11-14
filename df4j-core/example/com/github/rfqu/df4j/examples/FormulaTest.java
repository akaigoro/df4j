/*
 * Copyright 2011 by Alexei Kaigorodov
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package com.github.rfqu.df4j.examples;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.*;

import java.util.concurrent.ExecutionException;

import org.junit.Test;

import com.github.rfqu.df4j.core.Callback;
import com.github.rfqu.df4j.core.Promise;
import com.github.rfqu.df4j.core.Port;
import com.github.rfqu.df4j.core.CallbackFuture;
import com.github.rfqu.df4j.ext.BinaryOp;
import com.github.rfqu.df4j.ext.UnaryOp;

/**
 * Demonstration of building dataflow networks.
 * Sample networks realizing well-known mathematic formulae.
 * All network nodes execute in parallel, as soon as input data are ready.
 * Nodes communicate with each other using Port and Promise interfaces which have push semantics.
 * To convert push mechanism to more convenient pull mechanism, CallbackFuture objects are used.
 * Of course this examples have no practical meaning, as overheads for message passing 
 * overwhelms all gains from parallel execution.
 * For a dataflow network to have a sense, node execution should perform calculations
 * large enough to exceed the message handling time (which is less than 1 microsecond). 
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
        // typical scenario to run dataflow network is as follows.
        // instantiate desired network class:
        Square sq=new Square(); 
        // push argument values to inputs:
        sq.send(2.0); 
        // create a future to pull the result from the network
        CallbackFuture<Double> future = new CallbackFuture<Double>(sq);
       // network starts after both arguments and result consumers are defined
        // wait for the result
        Double res = future.get();
        // alternatively, use this shortcut:
//      Double res = CallbackFuture.getFrom(sq);
        // result is obtained 
        assertEquals(4, res.intValue());
    }

    /**
     * checks that execution exception is propagated
     */
    @Test
    public void t011() throws InterruptedException {
        Sqrt sq=new Sqrt(); 
        sq.send(-2.0); // square root from negative number would cause an error
        try {
            // that error manifests itself when the result is pulled from the network
            CallbackFuture.getFrom(sq);
            fail("no ExecutionException");
        } catch (ExecutionException e) {
            assertTrue( e.getCause() instanceof IllegalArgumentException);
        }
    }

    /**
     * computes 2*3
     */
    @Test
    public void t02() throws InterruptedException, ExecutionException {
		Mult node=new Mult();	
    	node.p1.send(2.0);
    	node.p2.send(3.0);
        assertEquals(6, CallbackFuture.getFrom(node).intValue());
    }

    /**
     * Complex network construction.
     * computes module of vector (a,b): sqrt(a^2+b^2)
     */
    @Test
    public void t03() throws InterruptedException, ExecutionException {
        // create nodes
        Square a=new Square();  
        Square b=new Square();  
        Sum sum = new Sum();
        Sqrt sq=new Sqrt(); 
        // create vertices
        a.addListener(sum.p1);        // a^2+b^2 -> sum
        b.addListener(sum.p2);
        sum.addListener(sq);         // sum -> sqrt
        // send arguments
        a.send(3.0);
        b.send(4.0);
        // wait for the result
        double res = CallbackFuture.getFrom(sq);
        assertEquals(5, res, delta);
    }

    /**
     * Demonstrates how complex network with single result
     * can be encapsulated in a class which extends Promise.
     * 
     * computes the discriminant of a quadratic equation
     *     D= b^2-4*a*c 
     */
    static class Discr extends Promise<Double> {
        // internal nodes
        private Mult mu2=new Mult();	
        private Diff diff = new Diff();
        // inputs
        MulByConst a=new MulByConst(4.0);   
        Square b=new Square();
        Callback<Double> c=mu2.p2;
		{
            a.addListener(mu2.p1);
            b.addListener(diff.p1);
            mu2.addListener(diff.p2);
            diff.addListener(this);
        }
	}
	
    /**
     * Demonstrates how complex network with 2 results
     * can be encapsulated in a class with 2 Promise members.
     * 
     * compute roots of a quadratic equation
     *     D  = b^2-4*a*c 
     *     x1 = (-b + sqrt(D))/(2*a) 
     *     x2 = (-b - sqrt(D))/(2*a) 
     */
	static class QuadEq {
        // internal nodes
	    private UnaryMinus mb=new UnaryMinus();
	    private Discr d =new Discr();
	    private Sqrt sqrt = new Sqrt();
	    private MulByConst mul=new MulByConst(2.0);    
	    private Sum sum = new Sum();
	    private Diff diff=new Diff();
        // inputs
        Promise<Double> a=new Promise<Double>(); // a and b used multiple times, require Promise
        Promise<Double> b=new Promise<Double>();
		Port<Double> c=d.c; // c is used only once
		// outputs
		Div x1 = new Div();
		Div x2 = new Div();
		// connections
        {
            a.addListener(d.a).addListener(mul);
            b.addListener(d.b).addListener(mb);
            d.addListener(sqrt);
            
            mb.addListener(sum.p1).addListener(diff.p1);
            sqrt.addListener(sum.p2).addListener(diff.p2);
            sum.addListener(x1.p1);
            mul.addListener(x1.p2).addListener(x2.p2);
            
            diff.addListener(x2.p1);
        }
	}

	/** checks evaluation of quadratic equation
	 */
    @Test
    public void t04() throws InterruptedException, ExecutionException {
        QuadEq node = new QuadEq();
        node.a.send(2.0);
        node.b.send(3.0);
        node.c.send(-14.0);

        assertEquals(2.0, CallbackFuture.getFrom(node.x1), delta);
        assertEquals(-3.5, CallbackFuture.getFrom(node.x2), delta);
    }

    /**
     * checks that execution exception is propagated
     */
    @Test
    public void t041() throws InterruptedException, ExecutionException {
        QuadEq node = new QuadEq();
        node.a.send(2.0);
        node.b.send(3.0);
        node.c.send(14.0);

        try {
            CallbackFuture.getFrom(node.x1).intValue();
            fail("no ExecutionException");
        } catch (ExecutionException e) {
            assertTrue( e.getCause() instanceof IllegalArgumentException);
        }
        try {
            CallbackFuture.getFrom(node.x2).intValue();
            fail("no ExecutionException");
        } catch (ExecutionException e) {
            assertTrue( e.getCause() instanceof IllegalArgumentException);
        }
    }
    
    // functional computing nodes

    static class Square extends UnaryOp<Double> {
        public Double eval(Double v) {
            return v * v;
        }
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

    static class MulByConst extends UnaryOp<Double> {
        private Double c;
        public MulByConst(Double c) {
            this.c = c;
        }
        public Double eval(Double v) {
            return c*v;
        }
    }
    
    /** another way to implement multiplication by a constant
     */
    static class MulByConst1 extends Mult implements Callback<Double>{
        public MulByConst1(Double c) {
            super.p1.send(c);
        }
        @Override
        public void send(Double value) {
            super.p2.send(value);
        }

        @Override
        public void sendFailure(Throwable exc) {
            super.p2.sendFailure(exc);
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
        qt.t041();
        /*
        qt.t02();
        qt.t03();
        qt.t04();
        */
    }
}
