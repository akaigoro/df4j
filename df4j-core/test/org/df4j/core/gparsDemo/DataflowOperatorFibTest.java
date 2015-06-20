package org.df4j.core.gparsDemo;

import java.math.BigInteger;

import org.df4j.core.actor.Actor;
import org.df4j.core.func.Promise;
import org.junit.Test;

/**
 * test taken from GPars - Groovy Parallel Systems
 * org.codehaus.gpars.javademo;
 * @author of original test Vaclav Pech
 * @author of the ported test Alexei Kaigorodov
 */
public class DataflowOperatorFibTest {
    static class FiboProcessor extends Actor {
        StreamInput<BigInteger> ch1=new StreamInput<BigInteger>();
        StreamInput<BigInteger> ch2=new StreamInput<BigInteger>();
        Promise<BigInteger> ch3=new Promise<BigInteger>(); 
        int counter = 0;
        
        @Override
        protected void act() {
            BigInteger a=ch1.get();
            BigInteger b=ch2.get();
            counter++;
            final BigInteger sum = a.add(b);
//            if (counter < 8) System.out.println(a+"+"+b+"="+sum);
            // if (counter == 1000000) {
            if (counter == 20000) {
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
        fibo.ch1.post(BigInteger.ONE);
        fibo.ch2.post(BigInteger.ZERO);
        fibo.ch2.post(BigInteger.ZERO);

        BigInteger val = fibo.ch3.get();
        System.out.println("result:"+val);

        System.out.println("time: " + (System.currentTimeMillis() - startTime));
    }
}
