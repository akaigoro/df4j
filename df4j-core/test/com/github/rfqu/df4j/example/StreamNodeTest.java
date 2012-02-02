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

import com.github.rfqu.df4j.core.*;
import com.github.rfqu.df4j.util.BinaryOp;
import com.github.rfqu.df4j.util.UnaryOp;


//node with single output stream
class StreamNode<T> {
 public static <T> void connect(InStreamPort<T> res, OutPort<T> sink) {
     res.connect(sink);
 }
}

class StreamNode1<T> extends StreamNode<T> implements InStreamPort<T>{
    DataSource<T> res;
    
    public void setRes(DataSource<T> res) {
        this.res=res;
    }

    @Override
    public void connect(OutStreamPort<T> sink) {
        res.connect(sink);
    }

    public T get() throws InterruptedException {
        Promise<T> pr=new Promise<T>();
        connect(pr);
        return pr.get();
    }

}

public class StreamNodeTest {
    private static final double delta = 1E-14;

	@Before
    public void init() {
        SimpleExecutorService executor = new SimpleExecutorService();
        Task.setCurrentExecutor(executor);
    }

    /**
     * compute a^2
     * 
     * @throws InterruptedException
     */
	class Node01 extends Node1<Double> {
		Square sq=new Square();	
		OutPort<Double> inp=sq;
		
        {
            setRes(sq);
        }
    };
    @Test
    public void t01() throws InterruptedException {
    	Node01 node = new Node01();
    	node.inp.send(2.0);
        int res = node.get().intValue();
        assertEquals(4, res);
    }

    /**
     * compute 2*3
     */
	class Node02 extends Node1<Double> {
		Mult mu=new Mult();	
		OutPort<Double> p1=mu.p1;
		OutPort<Double> p2=mu.p2;
        {
            setRes(mu);
        }
    };
    @Test
    public void t02() throws InterruptedException {
    	Node02 node = new Node02();
    	node.p1.send(2.0);
    	node.p2.send(3.0);
        assertEquals(6, node.get().intValue());
    }

    /**
     * compute sqrt(a^2+b^2)
     */
	class Node03 extends Node1<Double> {
		Square sq1=new Square();	
		Square sq2=new Square();	
        Sum sum = new Sum();
        Sqrt sqrt = new Sqrt();
		OutPort<Double> p1=sq1;
		OutPort<Double> p2=sq2;
        {
            connect(sq1, sum.p1);
            connect(sq2, sum.p2);
            connect(sum, sqrt);
            setRes(sqrt);
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
	class Discr extends Node1<Double> {
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
            connect(mu1, mu2.p1);
            connect(sq, diff.p1);
            connect(mu2, diff.p2);
            connect(diff, sqrt);
            setRes(sqrt);
        }
	}
    /**
     * compute roots of quadratic equation
     *     x1= (-b + D)/2a 
     *     x2= (-b - D)/2a 
     */
	class QuadEq extends Function<Double> {
		UnaryMinus mb=new UnaryMinus();
		Discr d =new Discr();
		Mult mul=new Mult();	
        Sum sum = new Sum();
        Diff diff=new Diff();
        Div div1=new Div();
        Div div2=new Div();
        // inputs
        DataSource<Double> a=new DataSource<Double>();
		DataSource<Double> b=new DataSource<Double>();
		OutPort<Double> c=d.c;
		// outputs
        Promise<Double> x1 = new Promise<Double>();
        Promise<Double> x2 = new Promise<Double>();
        
        {
            connect(a, d.a);
            connect(a, mul.p2);
            mul.p1.send(2.0);
            connect(b, d.b);
            connect(b, mb);
        	
            connect(mb, sum.p1);
            connect(d, sum.p2);
            connect(sum, div1.p1);
            connect(mul, div1.p2);
            connect(div1, x1);
        	
            connect(mb, diff.p1);
            connect(d, diff.p2);
            connect(diff, div2.p1);
            connect(mul, div2.p2);
            connect(div2, x2);
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
