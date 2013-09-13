package com.github.rfqu.df4j.ringbuffer;

import java.io.IOException;
import java.io.PrintStream;

//import org.junit.Assert;
import org.junit.Test;

import com.github.rfqu.df4j.core.DFContext;
import com.github.rfqu.df4j.ext.ImmediateExecutor;
import com.github.rfqu.df4j.ringbuffer.BoltNode.SourcePin;
import com.github.rfqu.df4j.testutil.IntValue;
import com.github.rfqu.df4j.testutil.MessageSink;

public class TransformerNodeTest {
    final static PrintStream out = System.out;
    final static PrintStream err = System.err;
    private static final int NUM_ACTORS = 100;
    private static final int NUM_TOKENS = 100;
    private static final int TIME_TO_LIVE = 10000;
    private int times=1;
    private int nThreads;

    @Test
    public void testFixed() throws InterruptedException {
        nThreads= Runtime.getRuntime().availableProcessors();
        DFContext.setFixedThreadPool(nThreads);
        runTest();
    }

    @Test
    public void testImm() throws InterruptedException {
        nThreads=1;
        DFContext.setCurrentExecutor(new ImmediateExecutor());
        runTest();
    }

    private void runTest() throws InterruptedException {
        String workerName = DFContext.getCurrentExecutor().getClass().getCanonicalName();
        out.println("<< Using " + workerName);
        out.printf("Ring with %d nodes, with %d tokens (%d ttl), on %d threads\n",
                NUM_ACTORS, NUM_TOKENS, TIME_TO_LIVE, nThreads);
        for (int i = 0; i < times; i++) {
            runTransformRing(1);
        }
    }
    
    /**
     * the core of the test
     */
    float runTransformRing(int batchSize) throws InterruptedException {
        MessageSink<Object> sink = new MessageSink<Object>(NUM_TOKENS);
        TransformerNode[] nodes = new TransformerNode[NUM_ACTORS];

        // create passing nodes
        for (int i = 0; i < nodes.length; i++) {
            nodes[i] = new TransformerNode(i, batchSize, sink);
        }
        // connect passing nodes
        for (int i = 0; i < nodes.length; i++) {
            IVRingBuffer rb=new IVRingBuffer(1);
            rb.connect(nodes[i].output, nodes[(i+1)%nodes.length].input);
            nodes[i].start();
        }
        SourcePin<IntValue> startPoint = nodes[0].output;
        long startTime = System.currentTimeMillis();
        // fire tokens
        for (int i = 0; i < NUM_TOKENS; i++) {
            startPoint.set(0, new IntValue(TIME_TO_LIVE));
            startPoint.shift(1);
        }
        
        // wait for all packets to die.
        sink.await();

        // report timings
        long elapsed = System.currentTimeMillis() - startTime;
        float throughput=(NUM_TOKENS*TIME_TO_LIVE*1000f/elapsed); // 1/ms
        float delay = 1.0e9f/throughput; //ns
        out.printf("elapsed=%,d ms, throughput=%,d, delay=%,d ns >>\n",
                elapsed, (int)throughput, (int)delay);
        return delay;
    }
    
    public static void main(String[] args) throws IOException, InterruptedException {
        TransformerNodeTest t= new TransformerNodeTest();
        t.testFixed();
    }
}
