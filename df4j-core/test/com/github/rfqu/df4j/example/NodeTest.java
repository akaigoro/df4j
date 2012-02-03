/*
 * Copyright 2011 by Alexei Kaigorodov
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package com.github.rfqu.df4j.example;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import com.github.rfqu.df4j.core.Connector;
import com.github.rfqu.df4j.core.OutPort;
import com.github.rfqu.df4j.core.Promise;
import com.github.rfqu.df4j.core.SimpleExecutorService;
import com.github.rfqu.df4j.core.Task;
import com.github.rfqu.df4j.util.BinaryOp;
import com.github.rfqu.df4j.util.Operation;
import com.github.rfqu.df4j.util.UnaryOp;

public class NodeTest {
    private static final double delta = 1E-14;

	@Before
    public void init() {
        SimpleExecutorService executor = new SimpleExecutorService();
        Task.setCurrentExecutor(executor);
    }

    /**
     * compute a^2
     */
    @Test
    public void t01() throws InterruptedException {
		Square sq=new Square();	
    	sq.send(2.0);
        int res = sq.get().intValue();
        assertEquals(4, res);
    }

    /**
     * compute 2*3
     */
    @Test
    public void t02() throws InterruptedException {
		Mult node=new Mult();	
    	node.p1.send(2.0);
    	node.p2.send(3.0);
        assertEquals(6, node.get().intValue());
    }

    /**
     * compute sqrt(a^2+b^2)
     */
	class Node03 extends Sqrt {
		Square sq1=new Square();	
		Square sq2=new Square();	
        Sum sum = new Sum();
		Square sq=new Square();	
		OutPort<Double> p1=sq1;
		OutPort<Double> p2=sq2;
        {
        	sq1.connect(sum.p1);
        	sq2.connect(sum.p2);
            sum.connect(sq);
            sq.connect(this);
        }
	}
	
    @Test
    public void t03() throws InterruptedException {
    	Node03 node = new Node03();
    	node.p1.send(3.0);
    	node.p2.send(4.0);
        double res = node.get();
        assertEquals(5, res, delta);
    }

    /**
     * compute the discriminant of a quadratic equation
     *     D= sqrt(b^2-4ac) 
     */
	class Discr extends Connector<Double> {
		Square sq=new Square();	
		Mult mu1=new Mult();	
		Mult mu2=new Mult();	
		Diff diff = new Diff();
        Sqrt sqrt = new Sqrt();
        // inputs
		OutPort<Double> a=mu1.p2;
		OutPort<Double> b=sq;
		OutPort<Double> c=mu2.p2;

		{
            mu1.p1.send(4.0);
            mu1.connect(mu2.p1);
            sq.connect(diff.p1);
            mu2.connect(diff.p2);
            diff.connect(sqrt);
            sqrt.connect(this);
        }
	}
    /**
     * compute roots of quadratic equation
     *     x1= (-b + D)/2a 
     *     x2= (-b - D)/2a 
     */
	class QuadEq {
		UnaryMinus mb=new UnaryMinus();
		Discr d =new Discr();
		Mult mul=new Mult();	
        Sum sum = new Sum();
        Diff diff=new Diff();
        Div div1=new Div();
        Div div2=new Div();
        // inputs
        Connector<Double> a=new Connector<Double>();
		Connector<Double> b=new Connector<Double>();
		OutPort<Double> c=d.c;
		// outputs
        Operation<Double> x1 = div1;
        Operation<Double> x2 = div2;
        
        {
            a.connect(d.a, mul.p2);
            b.connect(d.b, mb);
        	
            mb.connect(sum.p1, diff.p1);
            d.connect(sum.p2, diff.p2);
            sum.connect(div1.p1);
            mul.connect(div1.p2, div2.p2);
        	
            diff.connect(div2.p1);
            mul.p1.send(2.0);
        }

	}
    @Test
    public void t04() throws InterruptedException {
    	QuadEq node = new QuadEq();
    	node.a.send(2.0);
    	node.b.send(3.0);
    	node.c.send(-14.0);
        assertEquals(2.0, node.x1.get(), delta);
        assertEquals(-3.5, node.x2.get(), delta);
    }

    class Square extends UnaryOp<Double> {
        public Double operation(Double v) {
            return v * v;
        }
    }

    class Sqrt extends UnaryOp<Double> {
        public Double operation(Double v) {
            return Math.sqrt(v.doubleValue());
        }
    }

    class Sum extends BinaryOp<Double> {
        public Double operation(Double v1, Double v2) {
            return v1 + v2;
        }
    }

    class Mult extends BinaryOp<Double> {
        public Double operation(Double v1, Double v2) {
            return v1 * v2;
        }
    }

    class UnaryMinus extends UnaryOp<Double> {
        public Double operation(Double v) {
            return -v;
        }
    }

    class Diff extends BinaryOp<Double> {
        public Double operation(Double v1, Double v2) {
            return v1 - v2;
        }
    }

    class Div extends BinaryOp<Double> {
        public Double operation(Double v1, Double v2) {
            return v1 / v2;
        }
    }
}
