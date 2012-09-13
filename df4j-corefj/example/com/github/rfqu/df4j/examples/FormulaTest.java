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

public class FormulaTest {
    private static final double delta = 1E-14;
    
    /**
     * computes a^2
     * @throws InterruptedException 
     * @throws ExecutionException 
     */
    @Test
    public void t01() throws InterruptedException, ExecutionException {
        Square sq=new Square(); 
        sq.send(2.0);
        assertEquals(4, CallbackFuture.getFrom(sq).intValue());
    }

    /**
     * checks that execution exception is propagated
     */
    @Test
    public void t011() throws InterruptedException {
        Sqrt sq=new Sqrt(); 
        sq.send(-2.0);
        try {
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
     * computes sqrt(a^2+b^2)
     */
    static class Module extends Sqrt {
		Square sq1=new Square();	
		Square sq2=new Square();	
        Sum sum = new Sum();
		Port<Double> p1=sq1;
		Port<Double> p2=sq2;
        {
        	sq1.addListener(sum.p1);
        	sq2.addListener(sum.p2);
            sum.addListener(this);
        }
	}
	
    @Test
    public void t03() throws InterruptedException, ExecutionException {
    	Module node = new Module();
    	node.p1.send(3.0);
    	node.p2.send(4.0);
        double res = CallbackFuture.getFrom(node);
        assertEquals(5, res, delta);
    }

    /**
     * computes the discriminant of a quadratic equation
     *     D= b^2-4*a*c 
     */
    static class Discr extends Promise<Double> {
		Square sq=new Square();	
		Mult mu1=new Mult();	
		Mult mu2=new Mult();	
		Diff diff = new Diff();
        // inputs
		Callback<Double> a=mu1.p2;
		Callback<Double> b=sq;
		Callback<Double> c=mu2.p2;
		{
            mu1.p1.send(4.0);
            mu1.addListener(mu2.p1);
            sq.addListener(diff.p1);
            mu2.addListener(diff.p2);
            diff.addListener(this);
        }
	}
	
    /**
     * compute roots of a quadratic equation
     *     x1 = (-b + sqrt(D))/(2*a) 
     *     x2 = (-b - sqrt(D))/(2*a) 
     */
	static class QuadEq {
        // internal nodes
        UnaryMinus mb=new UnaryMinus();
        Discr d =new Discr();
        Sqrt sqrt = new Sqrt();
        Mult mul=new Mult();    
        Sum sum = new Sum();
        Diff diff=new Diff();
        // inputs
        Promise<Double> a=new Promise<Double>();
        Promise<Double> b=new Promise<Double>();
		Port<Double> c=d.c;
		// outputs
		Div x1 = new Div();
		Div x2 = new Div();
		// connections
        {
            a.addListener(d.a).addListener(mul.p2);
            b.addListener(d.b).addListener(mb);
            d.addListener(sqrt);
            
            mb.addListener(sum.p1).addListener(diff.p1);
            sqrt.addListener(sum.p2).addListener(diff.p2);
            sum.addListener(x1.p1);
            mul.addListener(x1.p2).addListener(x2.p2);
            
            diff.addListener(x2.p1);
            mul.p1.send(2.0);
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
