package com.github.rfqu.pipeline.core;

import static org.junit.Assert.*;
import java.nio.CharBuffer;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import org.df4j.pipeline.core.CopyTransformer;
import org.df4j.pipeline.core.Pipeline;
import org.df4j.pipeline.core.Pipeline.Connector;
import org.df4j.pipeline.df4j.core.DFContext;
import org.df4j.pipeline.df4j.ext.ImmediateExecutor;
import org.df4j.pipeline.util.CharBufSink;
import org.df4j.pipeline.util.CharBufSource;
import org.junit.BeforeClass;
import org.junit.Test;

public class PipelineTest {
	final static String string1 = "1";
	final static String string2 = "2";
	final static String string3 = string1+string2;
	
    @BeforeClass
    public static void initClass() {
        DFContext.setCurrentExecutor(new ImmediateExecutor());
    }
    
    void check1(CharBufSource source, LinkedBlockingQueue<String> res) throws InterruptedException {
        source.post("J");
        source.close();
        assertFalse(res.isEmpty());
        assertEquals("J", res.take());
    }

    void check2(CharBufSource source, LinkedBlockingQueue<String> res) throws InterruptedException {
        source.post(string1);
        source.post(string2);
        source.post("\n");
        assertFalse(res.isEmpty());
        assertEquals(string3, res.take());
        source.post(string1);
        source.post(string2);
        source.close();
        assertFalse(res.isEmpty());
        assertEquals(string3, res.take());
    }

    public void test(PipelineGenerator0 pg) throws InterruptedException, ExecutionException {
        pg.make(); check1(pg.source, pg.sink.getOutput());
        pg.make(); check2(pg.source, pg.sink.getOutput());
    }

    @Test
    public void test0() throws InterruptedException, ExecutionException {
        PipelineGenerator0 generator = new PipelineGenerator0();
        test(generator);
    }

    @Test
    public void test1() throws InterruptedException, ExecutionException {
        PipelineGenerator0 generator = new PipelineGenerator1();
        test(generator);
    }

    @Test
    public void test2() throws InterruptedException, ExecutionException {
        PipelineGenerator2 generator = new PipelineGenerator2();
        test(generator);
    }

    @Test
    public void testPostFailure() throws InterruptedException {
        Pipeline pipeline = new Pipeline();
        CharBufSource source = new CharBufSource();
        CharBufSink sink = new CharBufSink();

        MyCopyTransformer tf = new MyCopyTransformer();
        pipeline.setSource(source).addTransformer(tf).setSink(sink);
        assertFalse(pipeline.isDone());

        Throwable exc = new Throwable();
        tf.postFailure(exc);
        assertTrue(pipeline.isDone());
        try {
            pipeline.get();
            fail("exception expected");
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            assertEquals(exc, cause);
        }
    }

    // @Test
    public void testAll() throws InterruptedException, ExecutionException {
        test0();
        test1();
        test2();
    }

    class PipelineGenerator0 {
        Pipeline pipeline;
        CharBufSource source;
        CharBufSink sink;

        void make() throws InterruptedException, ExecutionException {
            pipeline = new Pipeline();
            source = new CharBufSource();
            sink = new CharBufSink();
            Connector<CharBuffer> connector = pipeline.setSource(source);
            connector=add(connector);
			connector.setSink(sink);
            pipeline.start();
        }
        
    	Connector<CharBuffer> add(Connector<CharBuffer> connector) throws InterruptedException, ExecutionException {
    		return connector;
        }
    }

    class PipelineGenerator1 extends PipelineGenerator0 {
    	Connector<CharBuffer> add(Connector<CharBuffer> connector) throws InterruptedException, ExecutionException {
            MyCopyTransformer tf = new MyCopyTransformer();
    		return connector.addTransformer(tf);
        }
    }

    class PipelineGenerator2 extends PipelineGenerator0 {
    	Connector<CharBuffer> add(Connector<CharBuffer> connector) throws InterruptedException, ExecutionException {
            MyCopyTransformer tf = new MyCopyTransformer();
            MyCopyTransformer tf2 = new MyCopyTransformer();
    		return connector.addTransformer(tf).addTransformer(tf2);
        }
    }

    class MyCopyTransformer extends CopyTransformer<CharBuffer> {
        void postFailure(Throwable exc) {
            context.postFailure(exc);
        }
    }
}
