package org.df4j.core.port.reverseflow;

import org.df4j.core.actor.Actor;
import org.df4j.core.port.InpChannel;
import org.df4j.core.util.Utils;
import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.CompletionException;

public class InpChannelTest {
    static class TestActor extends Actor {
        int runCounter = 0;
        @Override
        protected void runAction() {
            runCounter++;
            suspend();
        }
    }

    @Test
    public void simpleTest() throws CompletionException {
        int cnt = 3;
        TestActor actor = new TestActor();
        InpChannel<Integer> inp = new InpChannel<>(actor, 2);
        actor.setExecutor(Utils.directExec);
        actor.start();
        Assert.assertFalse(inp.isReady());
        inp.add(1);
        Assert.assertEquals(1, actor.runCounter);
        Assert.assertTrue(inp.isReady());
        inp.add(2);
        try {
            inp.add(3);
        } catch (Exception e) {
            Assert.assertTrue(e instanceof IllegalStateException);
        }
        Assert.assertEquals(1, actor.runCounter);
        actor.resume();
        Assert.assertEquals(2, actor.runCounter);
        Assert.assertEquals(1, inp.remove().intValue());
        Assert.assertTrue(inp.isReady());
        Assert.assertEquals(2, inp.remove().intValue());
        Assert.assertFalse(inp.isReady());
        try {
            inp.remove();
        } catch (Exception e) {
            Assert.assertTrue(e instanceof IllegalStateException);
        }
    }
}

