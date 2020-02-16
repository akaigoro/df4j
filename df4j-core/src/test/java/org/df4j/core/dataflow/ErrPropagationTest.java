package org.df4j.core.dataflow;

import org.df4j.core.util.CurrentThreadExecutor;
import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.CompletionException;

public class ErrPropagationTest {
    static class StringToInt extends AsyncFunc<Integer> {
        String argumnet;

        // in constructor, link this async function to a dataflow
        public StringToInt(Dataflow df, String argumnet) {
            super(df);
            this.argumnet = argumnet;
        }

        @Override
        protected Integer callAction() {
            Integer res = Integer.valueOf(argumnet);  // can throw NumberFormatException
    //        System.out.println(portsToString());
            return res;
        }
    }

    @Test
    public void test1() throws InterruptedException {
        Dataflow upper = new Dataflow();
        Dataflow nested = new Dataflow(upper);
        StringToInt nodeOK = new StringToInt(nested, "10");
        StringToInt nodeBad = new StringToInt(nested, "not an integer");
        nodeBad.start();
        nodeOK.start();
        try {
            upper.blockingAwait(100);
            Assert.fail("exception expected");
        } catch (CompletionException e) {
            System.err.println(e);
            Assert.assertEquals(NumberFormatException.class, e.getCause().getClass());
        }
    }

    @Test
    public void portsToStringTest() {

    }
}
