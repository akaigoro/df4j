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

import java.util.concurrent.ExecutorService;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.github.rfqu.df4j.core.Connector;
import com.github.rfqu.df4j.core.Port;
import com.github.rfqu.df4j.core.PortFuture;
import com.github.rfqu.df4j.core.SimpleExecutorService;
import com.github.rfqu.df4j.core.Task;
import com.github.rfqu.df4j.util.BinaryOp;
import com.github.rfqu.df4j.util.UnaryOp;

public class FormulaTest {
    private static final double delta = 1E-14;
    ExecutorService executor = new SimpleExecutorService();

	@Before
    public void init() {
        Task.setCurrentExecutor(executor);
    }
	
	@After
	public void deinit() {
	    executor.shutdownNow();
	}

    /**
     * compute a^2
     */
    @Test
    public void t01() throws InterruptedException {
		Square sq=new Square();	
	    PortFuture<Double> pr=new PortFuture<Double>();
    	sq.connect(pr);
    	sq.send(2.0);
        int res = pr.get().intValue();
        assertEquals(4, res);
    }

    /**
     * compute 2*3
     */
    @Test
    public void t02() throws InterruptedException {
		Mult node=new Mult();	
	    PortFuture<Double> pr=new PortFuture<Double>();
    	node.connect(pr);
    	node.p1.send(2.0);
    	node.p2.send(3.0);
        assertEquals(6, pr.get().intValue());
    }

    /**
     * compute sqrt(a^2+b^2)
     */
	class Module extends Sqrt {
		Square sq1=new Square();	
		Square sq2=new Square();	
        Sum sum = new Sum();
		Port<Double> p1=sq1;
		Port<Double> p2=sq2;
        {
        	sq1.connect(sum.p1);
        	sq2.connect(sum.p2);
            sum.connect(this);
        }
	}
	
    @Test
    public void t03() throws InterruptedException {
    	Module node = new Module();
	    PortFuture<Double> pr=new PortFuture<Double>();
    	node.connect(pr);
    	node.p1.send(3.0);
    	node.p2.send(4.0);
        double res = pr.get();
        assertEquals(5, res, delta);
    }

    /**
     * compute the discriminant of a quadratic equation
     *     D= b^2-4*a*c 
     */
	class Discr extends Connector<Double> {
		Square sq=new Square();	
		Mult mu1=new Mult();	
		Mult mu2=new Mult();	
		Diff diff = new Diff();
        // inputs
		Port<Double> a=mu1.p2;
		Port<Double> b=sq;
		Port<Double> c=mu2.p2;
		{
            mu1.p1.send(4.0);
            mu1.connect(mu2.p1);
            sq.connect(diff.p1);
            mu2.connect(diff.p2);
            diff.connect(this);
        }
	}
    /**
     * compute roots of quadratic equation
     *     x1 = (-b + sqrt(D))/(2*a) 
     *     x2 = (-b - sqrt(D))/(2*a) 
     */
	@SuppressWarnings("unchecked")
    class QuadEq {
		UnaryMinus mb=new UnaryMinus();
		Discr d =new Discr();
        Sqrt sqrt = new Sqrt();
		Mult mul=new Mult();	
        Sum sum = new Sum();
        Diff diff=new Diff();
        // inputs
        Connector<Double> a=new Connector<Double>();
        Connector<Double> b=new Connector<Double>();
		Port<Double> c=d.c;
		// outputs
		Div x1 = new Div();
		Div x2 = new Div();      
        {
            a.connect(d.a, mul.p2);
            b.connect(d.b, mb);
            d.connect(sqrt);
        	
            mb.connect(sum.p1, diff.p1);
            sqrt.connect(sum.p2, diff.p2);
            sum.connect(x1.p1);
            mul.connect(x1.p2, x2.p2);
        	
            diff.connect(x2.p1);
            mul.p1.send(2.0);
        }

	}
    @Test
    public void t04() throws InterruptedException {
    	QuadEq node = new QuadEq();
	    PortFuture<Double> pr1=new PortFuture<Double>();
	    PortFuture<Double> pr2=new PortFuture<Double>();
	    node.x1.connect(pr1);
	    node.x2.connect(pr2);
    	node.a.send(2.0);
    	node.b.send(3.0);
    	node.c.send(-14.0);
        assertEquals(2.0, pr1.get(), delta);
        assertEquals(-3.5, pr2.get(), delta);
    }

    class Square extends UnaryOp<Double> {
        public Double eval(Double v) {
            return v * v;
        }
    }

    class Sqrt extends UnaryOp<Double> {
        public Double eval(Double v) {
            return Math.sqrt(v.doubleValue());
        }
    }

    class Sum extends BinaryOp<Double> {
        public Double eval(Double v1, Double v2) {
            return v1 + v2;
        }
    }

    class Mult extends BinaryOp<Double> {
        public Double eval(Double v1, Double v2) {
            return v1 * v2;
        }
    }

    class UnaryMinus extends UnaryOp<Double> {
        public Double eval(Double v) {
            return -v;
        }
    }

    class Diff extends BinaryOp<Double> {
        public Double eval(Double v1, Double v2) {
            return v1 - v2;
        }
    }

    class Div extends BinaryOp<Double> {
        public Double eval(Double v1, Double v2) {
            return v1 / v2;
        }
    }
    
    public static void main(String args[]) throws InterruptedException {
    	FormulaTest qt = new FormulaTest();
        qt.init();
        qt.t01();
        qt.t02();
        qt.t03();
        qt.t04();
        qt.deinit();
    }
}
