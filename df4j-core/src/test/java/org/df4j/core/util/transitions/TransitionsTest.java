package org.df4j.core.util.transitions;

import org.df4j.core.dataflow.Actor;
import org.df4j.core.port.InpFlow;
import org.junit.Ignore;
import org.junit.Test;

public class TransitionsTest {
    static class TransitionActor extends Actor {
        @Transitions
        InpFlow inp = new InpFlow(this);

        TransitionActor() {
            TransitionActor.class.getAnnotations();
        }

        @Transition
        @Override
        protected void runAction() throws Throwable {

        }
    }

    @Ignore
    @Test
    public void test() {
        TransitionScanner.findTransitions(TransitionActor.class);
    }
}
