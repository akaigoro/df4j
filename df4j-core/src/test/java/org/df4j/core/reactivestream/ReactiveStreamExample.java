/*
 * Copyright 2012 by Alexei Kaigorodov
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.df4j.core.reactivestream;

import org.df4j.core.boundconnector.reactivestream.*;
import org.df4j.core.tasknode.Action;
import org.df4j.core.tasknode.AsyncProc;
import org.df4j.core.tasknode.messagescalar.AllOf;
import org.df4j.core.tasknode.messagestream.Actor;
import org.junit.Before;
import org.junit.Test;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.io.PrintStream;
import java.util.concurrent.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ReactiveStreamExample extends AllOf {
    @Before
    public void init() {
        AsyncProc.setThreadLocalExecutor(AsyncProc.currentThreadExec);
    }

    public void testSourceToSink(int sourceNumber, int sinkNumber) throws Exception {
        Source from = new Source(sourceNumber);
        Sink to1 = new Sink(sinkNumber);
        from.subscribe(to1);
        Sink to2 = new Sink(sinkNumber);
        from.subscribe(to2);
        super.start(); // after all components created
        from.start();
        asyncResult().get(1, TimeUnit.SECONDS);
        // publisher always sends all tokens, even if all subscribers unsubscribed.
        sinkNumber = Math.min(sourceNumber, sinkNumber);
        assertEquals(sinkNumber, to1.received);
        assertEquals(sinkNumber, to2.received);
    }

    @Test
    public void testSourceFirst() throws Exception {
        testSourceToSink(0, 1);
        testSourceToSink(2, 1);
    }

    @Test
    public void testSinkFirst() throws Exception {
        testSourceToSink(1, 0);
        testSourceToSink(10, 11);
    }

    @Test
    public void testSameTime() throws Exception {
        testSourceToSink(2, 2);


        testSourceToSink(0, 0);
        testSourceToSink(0, 1);
        testSourceToSink(1, 0);
        testSourceToSink(1, 1);
        testSourceToSink(1, 2);
        testSourceToSink(2, 1);
        testSourceToSink(2, 2);
        testSourceToSink(5, 5);
    }

    static PrintStream out = System.out;
    static void println(String s) {
        out.println(s);
        out.flush();
    }

    /**
     * emits totalNumber of Integers and closes the stream
     */
    class Source extends Actor implements Publisher<Integer> {
        ReactiveOutput<Integer> pub = new ReactiveOutput<>(this);
        int val = 0;

        public Source(int totalNumber) {
            this.val = totalNumber;
            registerAsyncResult(asyncResult());
        }

        @Override
        public void subscribe(Subscriber<? super Integer> subscriber) {
            pub.subscribe(subscriber);
        }

        @Action
        public void act() {
            if (val == 0) {
                pub.onComplete();
                println("Source.pub.onNext()");
                stop();
            } else {
                //          ReactorTest.println("pub.onNext("+val+")");
                pub.onNext(val);
                val--;
            }
        }
    }

    /**
     * receives totalNumber of Integers and cancels the subscription
     */
    class Sink extends Actor implements Subscriber<Integer> {
        int totalNumber;
        ReactiveInput<Integer> subscriber;
        int received = 0;

        public Sink(int totalNumber) {
            registerAsyncResult(asyncResult());
            if (totalNumber==0) {
                subscriber = new ReactiveInput<Integer>(this);
                subscriber.cancel();
                println("  sink: countDown");
                asyncResult().complete();
            } else {
                subscriber = new ReactiveInput<Integer>(this);
                this.totalNumber = totalNumber;
                start();
            }
        }

        @Override
        public void onSubscribe(Subscription subscription) {
            subscriber.onSubscribe(subscription);
        }

        @Override
        public void onNext(Integer message) {
            subscriber.onNext(message);
        }

        @Override
        public void onComplete() {
            subscriber.onComplete();
        }

        @Override
        public void onError(Throwable ex) {
            subscriber.onError(ex);
        }

        @Action
        public void act(Integer val) {
            //     ReactorTest.println("  Sink.current()="+val);
            if (val != null) {
                println("  sink: received "+val);
                received++;
                if (received < totalNumber) {
                    return;
                }
                subscriber.cancel();
            }
            println("  sink: countDown");
            stop();
        }
    }

}
