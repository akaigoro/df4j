package org.df4j.codec.javon.parser;

import static org.junit.Assert.assertEquals;
import java.io.IOException;

//import org.junit.Test;

import org.df4j.pipeline.codec.javon.builder.impl.JavonBuilder;
import org.df4j.pipeline.codec.javon.parser.JavonParser;
import org.df4j.pipeline.codec.json.builder.impl.JsonList;
import org.df4j.pipeline.codec.json.builder.impl.JsonMap;

public class ParserBuilderTest extends JavonParserTest {

    protected void check(String inp, String exp) throws IOException, Exception {
        JavonBuilder bd = new JavonBuilder();
        JavonParser mp=new JavonParser(bd);
        bd.put("A", A.class);
        bd.put("B", B.class);
        bd.put("D", D.class);
        Object obj = mp.parseFrom(inp);
        String res = obj.toString();
        assertEquals(exp, res);
    }

    public static class D extends JsonMap {

        public String toString() {
            StringBuilder sb=new StringBuilder();
            sb.append("D");
            if (!super.isEmpty()) {
                sb.append(super.toString());
            }
            return sb.toString();
        }
    }

    public static class B {
        int i;
        public B(int i) {this.i=i;}
        
        public String toString() {
            return "B("+i+")";
        }
    }
    
    public static class A extends JsonList {
        Integer i;
        Object a;

        public A() {
        }

        public A(Integer i) {
            this.i = i;
        }

        public Object getA() {
            return a;
        }

        public void setA(Object a) {
            this.a = a;
        }
        public String toString() {
            if (i==null && a==null) {
                return "A";
            }
            StringBuilder sb=new StringBuilder();
            sb.append("A(");
            if (i!=null) {
                sb.append(i);
            }
            if (a!=null) {
                if (i!=null) {
                    sb.append(',');
                }
                sb.append("a:");
                sb.append(a);
            }
            sb.append(')');
            if (!super.isEmpty()) {
                sb.append(super.toString());
            }
            return sb.toString();
        }
    }
}