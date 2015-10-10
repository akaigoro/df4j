package org.df4j.codec.javon.parser;

import static org.junit.Assert.assertEquals;
import java.io.IOException;
import org.df4j.pipeline.codec.javon.builder.impl.JavonPrinter;
import org.df4j.pipeline.codec.javon.parser.JavonParser;
import org.junit.Test;

public class JavonParserTest {

    @Test
    public void testObj() throws Exception {
        check("A");
        check("A()","A");
        check("A(1)","A(1)");
        check("A(0,a:1)","A(0,a:1)");
        check("A(a:1)","A(a:1)");
        check("A(a:B(1))","A(a:B(1))");
    }

    @Test
    public void testObjList() throws Exception {
        check("A(a:B(1))[null,4.0,true]");
    }

    @Test
    public void testMapList() throws Exception {
        check("D{\"z\":null,\"w\":1.0,\"ace\":true}");
        check("D(){ z:null, w:1.0, ace:D()}"
             ,"D{\"z\":null,\"w\":1.0,\"ace\":D}");
    }

    protected void check(String inp, String exp) throws IOException, Exception {
        JavonPrinter pr = new JavonPrinter();
        JavonParser mp=new JavonParser(pr);
        String res = mp.parseFrom(inp).toString();
        assertEquals(exp, res);
    }

    protected void check(String inp) throws IOException, Exception {
        check(inp, inp);
    }
}