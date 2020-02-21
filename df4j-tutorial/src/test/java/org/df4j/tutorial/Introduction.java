package org.df4j.tutorial;

import org.df4j.core.communicator.Completion;
import org.df4j.core.dataflow.Activity;
import org.df4j.core.dataflow.Actor;
import org.df4j.core.dataflow.AsyncProc;
import org.df4j.protocol.Completable;
import org.junit.Assert;
import org.junit.Test;

import java.io.PrintStream;

/**
 * this is a set of code snippets from simple to complex.
 * To run them, run junit tests
 */
public class Introduction<out> {

    /**
     * Simplest asynchronous procedure
     */
    class HelloWorld extends AsyncProc {
        @Override
        protected void runAction() throws Throwable {
            out.println("Hello, world!");
        }
    }

    @Test
    public void test1() {
        HelloWorld helloWorld = new HelloWorld();
        helloWorld.start();
    }

    /**
     * Simplest {@link Actor}
     * By default, actors run forever, repeating its {@link #runAction()} method.
     * So each actor must have conditions to stop execution.
     */
    class HelloWorldActor extends Actor {
        int maxCount = 10;

        @Override
        protected void runAction() throws Throwable {
            out.println("Hello, world!");
            if (--maxCount == 0) {
                onComplete();   // stops actor's execution
            }
        }
    }

    @Test
    public void test2() {
        HelloWorldActor helloWorld = new HelloWorldActor();
        startAndWait(helloWorld);
    }

    static PrintStream out = System.out;

    public static void startAndWait(Activity activity) {
        activity.start();
        boolean fin = activity.blockingAwait(400);
        Assert.assertTrue(fin);
    }
}
