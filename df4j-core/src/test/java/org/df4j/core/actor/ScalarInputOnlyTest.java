package org.df4j.core.actor;

import org.df4j.core.asyncproc.ScalarInput;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ScalarInputOnlyTest {

    @Test
    public void test1() throws InterruptedException {
        ScalarInputActor actor = new ScalarInputActor();
        actor.start();
        actor.inp.onComplete(33);
    }

    static class ScalarInputActor extends Actor {
        ScalarInput<Integer> inp = new ScalarInput<>(this);
        int cnt = 0;

        @Override
        protected void runAction() throws Throwable {
            System.out.println("cnt = "+cnt+": inp="+inp.current());
            assertEquals(0, cnt); // runAction must be called only once, because ScalarInput is completed
            cnt++;
        }
    }
}
