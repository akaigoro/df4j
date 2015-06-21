package com.github.rfqu.codec.javon.pushparser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import java.io.IOException;
import org.df4j.pipeline.codec.javon.builder.impl.JavonPrinter;
import org.df4j.pipeline.codec.javon.pushparser.JavonParser;
import org.df4j.pipeline.core.Pipeline;
import org.df4j.pipeline.df4j.core.CompletableFuture;
import org.df4j.pipeline.util.CharBufSource;
import org.junit.Before;
import org.junit.Test;

public class so1688099test extends com.github.rfqu.codec.javon.parser.so1688099test {
    JavonPrinter pr = new JavonPrinter();
    JavonParser tp=new JavonParser(pr);
    CharBufSource source = new CharBufSource();
    CompletableFuture<Object> future;
    
    @Before
    public void init() throws Exception {
        future=new Pipeline()
        .setSource(source)
        .setSink(tp)
        .start();
    }

    @Test
    public void testWithPrinter() throws IOException, Exception {
        source.post(inp);
        source.close();
        assertTrue(future.isDone());
        String resS = future.get().toString();
		compareStrings(inp, resS);
        assertEquals(inp, resS);
	}

	@Test
    public void testWithBuilder() throws IOException, Exception {
        source.post(inp);
        source.close();
        assertTrue(future.isDone());
        String resS = future.get().toString();
		compareStrings(inp, resS);
        assertEquals(inp, resS);
	}
}
