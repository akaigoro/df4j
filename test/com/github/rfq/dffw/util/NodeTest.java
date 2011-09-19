package com.github.rfq.dffw.util;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

import com.github.rfq.dffw.core.Worker;
import com.github.rfq.dffw.util.BinaryOp;
import com.github.rfq.dffw.util.RunnablePromise;
import com.github.rfq.dffw.util.UnaryOp;

public class NodeTest {
    Worker exec=new Worker();

    /**
     * compute a^2
     * @throws InterruptedException 
     */
    @Test
    public void t01() throws InterruptedException{
    	RunnablePromise<Integer> command=new RunnablePromise<Integer>(){
            @Override
    		public void run() {
        		new Square().send(2).res.request(this);
    		}
    	};
    	exec.start();
    	exec.execute(command);
        int res=command.get();
        assertEquals(4, res);
    }
    
    /**
     * compute 2*3
     */
    @Test
    public void t02() throws InterruptedException {
    	RunnablePromise<Integer> command=new RunnablePromise<Integer>(){
            @Override
    		public void run() {
        		new Mult().p1.send(2).p2.send(3).res.request(this);
    		}
        };
       	exec.start();
    	exec.execute(command);
        int res=command.get();
        assertEquals(6, res);
    }
    
    /**
     * compute sqrt(a^2+b^2)
     */
    @Test
    public void t03() throws InterruptedException {
    	exec.start();
    	RunnablePromise<Double> command=new RunnablePromise<Double>(){
            @Override
    		public void run() {
        		Sum sum=new Sum();
        		new Square().send(3).res.request(sum.p1);
        		new Square().send(4).res.request(sum.p2);
        		sum.res.request(new Sqrt()).res.request(this);
    		}
    	};
    	exec.execute(command);
        double res=command.get();
        assertEquals(5, res, 0.00001);
    }

    class Square extends UnaryOp<Integer, Integer> {
        public Integer call(Integer v) {
            return v*v;
        }
    }

    class Sqrt extends UnaryOp<Integer, Double> {
        public Double call(Integer v) {
            return Math.sqrt(v.doubleValue());
        }
    }

    class Sum extends BinaryOp<Integer> {
        public Integer call(Integer v1, Integer v2) {
            return v1+v2;
        }
    }

    class Mult extends BinaryOp<Integer> {
        public Integer call(Integer v1, Integer v2) {
            return v1*v2;
        }
    }
}
