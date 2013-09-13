package com.github.rfqu.df4j.ringbuffer;

import java.io.IOException;
import java.io.PrintStream;

//import org.junit.Assert;
import org.junit.Test;

import com.github.rfqu.df4j.core.DFContext;
import com.github.rfqu.df4j.ext.ImmediateExecutor;
import com.github.rfqu.df4j.testutil.MessageSink;

public class BoltNodeTest {
    final static PrintStream out = System.out;

    @Test
    public void smokeTest() throws InterruptedException {
        MessageSink<Object> context = new MessageSink<Object>(1);
        SourceNode source = new SourceNode(context);
        SinkNode sink = new SinkNode(context);
        IVRingBuffer rb = new IVRingBuffer();
        rb.fill();
        rb.connect(source.output, sink.input);
        sink.start();
        source.start(2);
        context.await();
    }
    
    void test(int iterations, int batchSize) throws InterruptedException {
        String exec=DFContext.getCurrentExecutor().getClass().getSimpleName();
        System.out.printf("<<exec=%s; iterations=%,d; batchSize=%,d\n", exec, iterations, batchSize);
        MessageSink<Object> context = new MessageSink<Object>(1);
        SourceNode source = new SourceNode(context);
        SinkNode sink = new SinkNode(context);
        IVRingBuffer rb = new IVRingBuffer();
        rb.fill();
        rb.connect(source.output, sink.input);
        sink.start();
        long start=System.currentTimeMillis();
        source.start(iterations);
        context.await();
        long end=System.currentTimeMillis();
        long elapsed = end-start;
        float throughput=iterations/(elapsed/1000f);
        System.out.printf("elapsed=%,d ms, throughput=%,d>>\n", elapsed, (int)throughput);
    }

//    @Test
    public void test1() throws InterruptedException {
        DFContext.setCurrentExecutor(new ImmediateExecutor());
        int iterations = 1000000;
        test(iterations/2, 1);
        test(iterations, 1);
        test(iterations, 3);
        test(iterations, 10);
        test(iterations, 30);
        test(iterations, 100);
    }

//    @Test
    public void test2() throws InterruptedException {
        int iterations = 1000000;
        test(iterations/2, 1);
        test(iterations, 1);
        test(iterations, 100);
        test(iterations, 200);
        test(iterations, 300);
        test(iterations, 400);
        test(iterations, 500);
        test(iterations, 600);
        test(iterations, 700);
        test(iterations, 1000);
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        BoltNodeTest t= new BoltNodeTest();
//        System.in.read();
//        t.testFixed();
        t.smokeTest();
    }
}
