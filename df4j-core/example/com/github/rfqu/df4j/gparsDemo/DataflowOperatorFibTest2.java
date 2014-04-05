package com.github.rfqu.df4j.gparsDemo;

import org.junit.Test;

import com.github.rfqu.df4j.core.CompletableFuture;
import com.github.rfqu.df4j.core.DataflowNode;

/**
 * test taken from GPars - Groovy Parallel Systems
 * org.codehaus.gpars.javademo;
 * @author of original test Vaclav Pech
 * @author of the ported test Alexei Kaigorodov
 */
public class DataflowOperatorFibTest2 {
    static class FiboProcessor extends DataflowNode {
        StreamInput<Integer> ch1=new StreamInput<Integer>();
        StreamInput<Integer> ch2=new StreamInput<Integer>();
        CompletableFuture<Integer> ch3=new CompletableFuture<Integer>(); 
        long counter = 0L;
        
        @Override
        protected void act() {
            Integer a=ch1.get();
            Integer b=ch2.get();
            counter++;
            final Integer sum = a+b;
            if (counter == 1000000L) {
                ch3.post(sum);
            } else {
                ch1.post(sum);
                ch2.post(sum);
            }
        }
    }

    @Test
    public void testFibonacci() throws Exception {
        final FiboProcessor fibo = new FiboProcessor();

        final long startTime = System.currentTimeMillis();
        fibo.ch1.post(1);
        fibo.ch2.post(0);
        fibo.ch2.post(0);

        Integer val = fibo.ch3.get();
        System.out.println("result:"+val);

        System.out.println("time: " + (System.currentTimeMillis() - startTime));
    }
}
