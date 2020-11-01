package org.df4j.core.actor;

import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.CompletionException;

public class ErrPropagationTest {
    static class StringToInt extends AsyncFunc<Integer> {
        String argumnet;

        public StringToInt(ActorGroup df, String argumnet) {
            super(df);
            this.argumnet = argumnet;
        }

        @Override
        protected Integer callAction() {
            Integer res = Integer.valueOf(argumnet);  // can throw NumberFormatException
            return res;
        }
    }

    @Test
    public void test1() throws InterruptedException {
        ActorGroup upper = new ActorGroup();
        ActorGroup nested = new ActorGroup(upper);
        StringToInt nodeOK = new StringToInt(nested, "10");
        StringToInt nodeBad = new StringToInt(nested, "not an integer");
        nodeBad.start();
        nodeOK.start();
        try {
            boolean res = upper.await(40000);
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
