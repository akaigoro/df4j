package org.df4j.core.gparsDemo;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

import java.util.concurrent.ArrayBlockingQueue;

import org.df4j.core.actor.Actor;
import org.df4j.core.actor.StreamInput;
import org.junit.Test;

/**
 * test taken from GPars - Groovy Parallel Systems
 * org.codehaus.gpars.javademo;
 * @author of original test Vaclav Pech, Lukas Krecan, Pavel Jetensky, Michal Franc
 */
public class DataflowOperatorTest {
    static class Doubler extends Actor {
        StreamInput<Integer> stream1=new StreamInput<Integer>();
        StreamInput<Integer> stream2;
        
        @Override
        protected void act() {
            stream2.post(2 * stream1.get());
        }
    }

    static class Summator extends Actor {
        StreamInput<Integer> stream2=new StreamInput<Integer>();
        StreamInput<Integer> stream3=new StreamInput<Integer>();
        ArrayBlockingQueue<Integer> stream4;
        
        @Override
        protected void act() {
            try {
                stream4.put(stream2.get()+stream3.get());
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    @Test
    public void testFlow() throws Exception {
        // processor1 waits for value in stream 1 and writes 2*value to stream2
        final Doubler processor1 = new Doubler();

        // processor2 reads value from stream2 and stream 3 and writes sum to stream 4
        final Summator processor2 = new Summator();

        final StreamInput<Integer> stream1 = processor1.stream1;
        final StreamInput<Integer> stream3 = processor2.stream3;
        final ArrayBlockingQueue<Integer> stream4 = new ArrayBlockingQueue<Integer>(5);

        // make connections
        processor1.stream2=processor2.stream2;
        processor2.stream4=stream4;

        // Multiple values can be send to one stream
        stream1.post(1);
        stream1.post(2);
        stream1.post(3);

        // processor processor2 waits for stream3 values, lets send them
        stream3.post(100);
        stream3.post(100);
        stream3.post(100);

        //fetch values
        assertEquals(102, stream4.take().intValue());
        assertEquals(104, stream4.take().intValue());
        assertEquals(106, stream4.take().intValue());

        assertTrue("All values fetched, no output expected", stream4.isEmpty());
    }
}

