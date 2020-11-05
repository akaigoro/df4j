package org.df4j.core.port;

import org.df4j.core.actor.Actor;
import org.df4j.core.util.Utils;
import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.CompletionException;

public class InpMultiFlowTest {

    static class TestActor extends Actor {
        InpMultiFlow<Integer> inp = new InpMultiFlow<>(this, 2);
        int value;
        int runCounter = 0;

        @Override
        protected void runAction() {
            value = inp.remove();
            runCounter++;
            suspend();
        }
    }

    @Test
    public void simpleTest() throws CompletionException, InterruptedException {
        int cnt = 3;
        TestActor actor = new TestActor();
        InpMultiFlow<Integer> inp = actor.inp;
        actor.setExecutor(Utils.directExec);
        Assert.assertFalse(inp.isReady());
        inp.add(1);
        Assert.assertEquals(0, actor.runCounter);
        Assert.assertTrue(inp.isReady());
        inp.add(2);
        try {
            inp.add(3);
            Assert.fail("buffer overflow expected");
        } catch (Exception e) {
            Assert.assertTrue(e instanceof IllegalStateException);
        }
        actor.start();
        Thread.sleep(100);
        Assert.assertEquals(1, actor.runCounter);
        Assert.assertEquals(1, actor.value);
        actor.resume();
        Assert.assertEquals(2, actor.runCounter);
        Assert.assertEquals(2, actor.value);
        inp.add(3);
        Assert.assertTrue(inp.isReady());
        actor.resume();
        Thread.sleep(100);
        Assert.assertFalse(inp.isReady());
        try {
            inp.remove();
        } catch (Exception e) {
            Assert.assertTrue(e instanceof IllegalStateException);
        }
    }
}

