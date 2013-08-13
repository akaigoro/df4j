package com.github.rfqu.df4j.pipeline;

import static org.junit.Assert.*;
import org.junit.BeforeClass;
import org.junit.Test;

import com.github.rfqu.df4j.core.DFContext;
import com.github.rfqu.df4j.ext.ImmediateExecutor;

public class ChunkTest {
	final static String string1 = "1st str";
	final static String string2 = "2nd str";
	final static String string3 = string1+string2;
	
	@BeforeClass
	public static void init() {
		DFContext.setCurrentExecutor(new ImmediateExecutor());
	}
	
	CharChunkSource source;
	CharChunkSink sink;
	
	private void check() {
		source.post(string1);
		source.post(string2);
		assertFalse(sink.isClosed());
		source.close();
//		assertTrue(sink.isClosed());
		assertEquals(string3, sink.getRes());
	}
	
	@Test
	public void tSource2Sink() {
		source=new CharChunkSource();
		sink=new CharChunkSink(source);
		check();
	}

	@Test
	public void tNullTransform1() {
		source=new CharChunkSource();
		CharChunkNullTransform tf=new CharChunkNullTransform(source, 4);
		sink=new CharChunkSink(tf);
		check();
	}
	
	@Test
	public void tNullTransform1L() {
		source=new CharChunkSource();
		CharChunkNullTransform tf=new CharChunkNullTransform(source, 4000);
		sink=new CharChunkSink(tf);
		check();
	}
	
	@Test
	public void tNullTransform2() {
		source=new CharChunkSource();
		CharChunkNullTransform tf=new CharChunkNullTransform(source, 1);
		CharChunkNullTransform tf2=new CharChunkNullTransform(tf, 2);
		sink=new CharChunkSink(tf2);
		check();
	}
}
