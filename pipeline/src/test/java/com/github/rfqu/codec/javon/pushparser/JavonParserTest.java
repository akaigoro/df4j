package com.github.rfqu.codec.javon.pushparser;

import static org.junit.Assert.*;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import org.df4j.pipeline.codec.javon.builder.impl.JavonPrinter;
import org.df4j.pipeline.codec.javon.pushparser.JavonParser;
import org.df4j.pipeline.codec.scanner.ParseException;
import org.df4j.pipeline.core.Pipeline;
import org.df4j.pipeline.df4j.core.CompletableFuture;
import org.df4j.pipeline.util.CharBufSource;
import org.junit.Before;
import org.junit.Test;

public class JavonParserTest {
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
    public void testObjN() throws Exception {
        checkN("A(a:1) A(a:1)");
        checkN("A(), []");
        checkN("A(1); [A(1)]");
        checkN("A(0,a:1) B{c:1}");
        checkN("A(a:B(1)) C[]");
    }

    @Test
    public void testObj() throws Exception {
        check("A");
        check("A()","A");
        check("A(1)");
        check("A(a,a,a:a,a:a)"); // ident both as positional arg and a key
        check("A(0,a:1)","A(0,a:1)");
        check("A(a:1)","A(a:1)");
        check("A(a:B(1))");
    }

    @Test
    public void testObjList() throws Exception {
        check("A[null,4.0,true]");
        check("A(a:B(1))[A{},4.0,true]");
    }

    @Test
    public void tesObjMap() throws Exception {
        check("A{}");
        check("A(){}","A{}");
        check("A(a:B(1)){\"b\":C(D(z:null))}");
        check("D{\"z\":null,\"w\":1.0,\"ace\":true}");
        check("D(){ z:null, w:1.0, ace:D()}"
                ,"D{\"z\":null,\"w\":1.0,\"ace\":D}");
    }

    @Test
    public void testObjMapList() throws Exception {
        check("D{\"z\":null,\"w\":1.0,\"ace\":true}[A{}]");
        check("D(){ z:null, w:1.0, ace:[D()]}[]"
             ,"D{\"z\":null,\"w\":1.0,\"ace\":[D]}[]");
    }

    protected void check(String inp, String exp) throws InterruptedException, ExecutionException  {
        source.post(inp);
        source.close();
        assertTrue(future.isDone());
        String resS = future.get().toString();
        assertEquals(exp, resS);
    }

    protected void checkN(String inp) throws IOException, Exception {
        source.post(inp);
        source.close();
        assertTrue(future.isDone());
        try {
			future.get();
			fail("ExecutionException expected");
		} catch (ExecutionException e) {
			ParseException pe=(ParseException) e.getCause();
			System.out.println(pe.getMessage());
		}
    }

    protected void check(String inp) throws IOException, Exception {
        check(inp, inp);
    }
    
    public static void main(String[] args) throws Exception {
        new JavonParserTest().testObj();
    }
}