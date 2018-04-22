package org.df4j.core.impl.examples.monitor;

import org.df4j.core.spi.messagescalar.Port;
import org.junit.Test;

/**
  Source => RingBuffer => Sink
 */
public class RingBufferTest {
    @Test
    public void test() {
        RingBuffer<Integer> rb=new RingBuffer<>(3);
        Port<Integer> printer = (Integer i)->System.out.println(i);

        for (int k=0; k<11; k++) {
            rb.post(rb.new Put(k){}); 
        }
        for (int k=0; k<11; k++) {
            rb.post(rb.new Get(printer){}); 
        }
        
    }

}
