package org.df4j.core.dataflow;

import org.df4j.core.port.InpScalar;
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
        protected Integer callAction() throws Throwable {
            return Integer.valueOf(argumnet); // can throw NumberFormatException
        }
    }

    @Test
    public void test1() throws InterruptedException {
        Dataflow upper = new Dataflow();
        Dataflow nested = new Dataflow(upper);
        new StringToInt(nested, "10").start();
        new StringToInt(nested, "not an integer").start();
        try {
            upper.blockingAwait(100);
            Assert.fail("exception expected");
        } catch (CompletionException e) {
            System.err.println(e);
            Assert.assertEquals(NumberFormatException.class, e.getCause().getClass());
        }
    }
}
