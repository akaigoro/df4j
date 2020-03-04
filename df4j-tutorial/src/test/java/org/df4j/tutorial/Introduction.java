package org.df4j.tutorial;

import org.df4j.core.dataflow.*;
import org.df4j.core.port.InpScalar;
import org.junit.Assert;
import org.junit.Test;

import java.io.PrintStream;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.Assert.fail;

/**
 *
 * I want to publish an article which reveals the anatomy of asynchronous programming, and show the place of the reactive streams amongst the other ways of communication of asynchronous activities.
 */

/**
 * this is a set of code snippets from simple to complex.
 * To run them, run junit tests
 */
public class Introduction<out> {
    /*================================================================ Utility methods*/

    static PrintStream out = System.out;

    /**
     * waits the activity (AsyncProc or Actor) to complete.
     * and suspends the caller method to let the asynchronous part of the test to finish.
     *
     * @param activity
     */
    public static void blockingAwait(Activity activity) {
        boolean fin = activity.blockingAwait(400);
        Assert.assertTrue(fin);
    }

    /**
     * waits the activity (AsyncProc or Actor) to complete.
     * and suspends the caller method to let the asynchronous part of the test to finish.
     *
     * @param activity
     */
    public static void expectException(Activity activity, Class<? extends Throwable> exceptionClass) {
        try {
            activity.blockingAwait(400);
            fail(" exception "+exceptionClass.getSimpleName()+" expected");
        } catch (CompletionException e) {
            e.printStackTrace();
            Throwable cause = e.getCause();
            Assert.assertTrue(exceptionClass.isAssignableFrom(cause.getClass()));
        }
    }

    /*================================================================*/
    /**
     * Simplest asynchronous procedure
     * Executes its {@link #runAction} method once.
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
        // suspend the test method, otherwise the test can stop prematurely
        blockingAwait(helloWorld);
    }

    /*================================================================*/
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
        helloWorld.start();
        // suspent the test method, otherwise the test can stop prematurely
        blockingAwait(helloWorld);
    }

    /*================================================================*/
    /**
     * Simplest asynchronous function
     * Executes its {@link #callAction} method once.
     */
    class HelloWorldSource extends AsyncFunc<String> {
        public HelloWorldSource() {
        }

        public HelloWorldSource(Dataflow dataflow) {
            super(dataflow);
        }

        @Override
        protected String callAction() throws Throwable {
            return "Hello, world!";
        }
    }

    @Test
    public void test3() throws TimeoutException {
        HelloWorldSource helloWorld = new HelloWorldSource();
        helloWorld.start();
        String result = helloWorld.get(1, TimeUnit.SECONDS);
        out.println("result: "+result);
    }

    /*================================================================*/
    /**
     * An asynchronous procedure with one input port
     */
    class Printer extends AsyncProc {
        InpScalar<String> inp = new InpScalar<>(this);

        public Printer() {
        }

        public Printer(Dataflow dataflow) {
            super(dataflow);
        }

        public Printer(InpScalar<String> inp) {
            this.inp = inp;
        }

        @Override
        protected void runAction() throws Throwable {
            // when runAction() is invoked, all port a ready.
            // input port has received a value:
            String str = inp.current();
            out.println("got: "+str);
        }
    }

    /**
     * Simplest dataflow graph:
     *
     * HelloWorldSource --> Printer
     */
    @Test
    public void test4() {
        // recommended scenario to work with graphs consists of 4 steps:
        // 1. create nodes
        HelloWorldSource helloWorld = new HelloWorldSource();
        Printer printer = new Printer();
        // 2. connect nodes
        helloWorld.subscribe(printer.inp);
        // 3. start nodes
        helloWorld.start();
        printer.start();
        // 4. wait for the end
        blockingAwait(printer);
    }

    /*================================================================*/
    /**
     * same nodes share single dataflow:
     *
     * HelloWorldSource --> Printer
     */
    @Test
    public void test5() {
        Dataflow df = new Dataflow();
        HelloWorldSource helloWorld = new HelloWorldSource(df);
        Printer printer = new Printer(df);
        helloWorld.subscribe(printer.inp);
        helloWorld.start();
        printer.start();
        // wait all nodes to complete
        blockingAwait(df);
    }

    /*================================================================*/
    /**
     * Asynchronous function with an error
     */
    class HelloWorldSourceErr extends AsyncFunc<String> {
        public HelloWorldSourceErr() {
        }

        public HelloWorldSourceErr(Dataflow dataflow) {
            super(dataflow);
        }

        @Override
        protected String callAction() throws Throwable {
            // divide by zero throws exception
            return Integer.toString(1/0);
        }
    }

    /**
     * the same nodes, but shared dataflow:
     *
     * HelloWorldSource --> Printer
     */
    @Test
    public void test6() {
        Dataflow df = new Dataflow();
        HelloWorldSourceErr helloWorld = new HelloWorldSourceErr(df);
        Printer printer = new Printer(df);
        helloWorld.subscribe(printer.inp);
        helloWorld.start();
        printer.start();
        // failure in any node causes failure of the whole graph.
        expectException(df, ArithmeticException.class);
    }
}
// TODO kill all nodes at failure