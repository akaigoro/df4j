package org.df4j.core.dataflow;

import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.CompletionException;

public class ErrPropagationTest {
    static class TestException extends RuntimeException{}

    static class ErrActor extends Actor {
        @Override
        protected void runAction() {
            throw new TestException();
        }
    }

    @Test
    public void test1() throws InterruptedException {
        ErrActor actor = new ErrActor();
        actor.start();
        try {
            actor.blockingAwait(100);
        } catch (CompletionException e) {
            Assert.assertEquals(TestException.class, e.getCause().getClass());
        }
    }
}
