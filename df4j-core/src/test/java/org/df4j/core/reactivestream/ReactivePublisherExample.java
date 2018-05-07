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

import org.df4j.core.connector.reactivestream.*;
import org.df4j.core.node.Actor;
import org.junit.Test;

import java.io.PrintStream;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ReactivePublisherExample {

    public void testSourceToSink(int sourceNumber, int sinkNumber) throws InterruptedException {
        CountDownLatch fin = new CountDownLatch(3);
        Source from = new Source(sourceNumber, fin);
        Sink to1 = new Sink(sinkNumber, fin);
        from.subscribe(to1);
        Sink to2 = new Sink(sinkNumber, fin);
        from.subscribe(to2);
        from.start();
        assertTrue(fin.await(2, TimeUnit.SECONDS));
        // publisher always sends all tokens, even if all subscribers unsubscribed.
        assertEquals(sourceNumber, from.sent);
        sinkNumber = Math.min(sourceNumber, sinkNumber);
        assertEquals(sinkNumber, to1.received);
        assertEquals(sinkNumber, to2.received);
    }

    @Test
    public void testSourceFirst() throws InterruptedException {
        testSourceToSink(0, 1);
        testSourceToSink(2, 1);
    }

    @Test
    public void testSinkFirst() throws InterruptedException {
        testSourceToSink(1, 0);
        testSourceToSink(10, 11);
    }

    @Test
    public void testSameTime() throws InterruptedException {
        testSourceToSink(0, 0);
        testSourceToSink(5, 5);
        testSourceToSink(50, 50);
    }

    static PrintStream out = System.out;
    static void println(String s) {
        out.println(s);
        out.flush();
    }

    /**
     * emits totalNumber of Integers and closes the stream
     */
    static class Source extends Actor implements Publisher<Integer> {
        ReactiveOutput<Integer> pub = new ReactiveOutput<>(this);
        int totalNumber;
        int val = 0;
        public int sent = 0;
        CountDownLatch fin;

        public Source(int totalNumber, CountDownLatch fin) {
            this.totalNumber = totalNumber;
            this.fin = fin;
        }

        @Override
        public void subscribe(Subscriber<? super Integer> subscriber) {
            pub.subscribe(subscriber);
        }

        @Override
        protected void act() {
            if (val >= totalNumber) {
                pub.complete();
                println("pub.complete()");
                fin.countDown();
                stop();
            } else {
                //          ReactorTest.println("pub.post("+val+")");
                pub.post(val);
                sent++;
                val++;
            }
        }
    }

    /**
     * receives totalNumber of Integers and cancels the subscription
     */
    static class Sink extends Actor implements Subscriber<Integer> {
        int totalNumber;
        ReactiveInput<Integer> subscriber;
        final CountDownLatch fin;
        int received = 0;

        public Sink(int totalNumber, CountDownLatch fin) {
            this.fin = fin;
            if (totalNumber==0) {
                subscriber = new ReactiveInput<Integer>(this);
                subscriber.cancel();
                println("  sink: countDown");
                fin.countDown();
                return;
            }
            subscriber = new ReactiveInput<Integer>(this);
            this.totalNumber = totalNumber;
            start();
        }

        @Override
        public void onSubscribe(Subscription subscription) {
            subscriber.onSubscribe(subscription);
        }

        @Override
        public void post(Integer message) {
            subscriber.post(message);
        }

        @Override
        public void postFailure(Throwable ex) {
            subscriber.postFailure(ex);
        }

        @Override
        public void complete() {
            subscriber.complete();
        }

        @Override
        protected void act() {
            Integer val = subscriber.current();
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
            fin.countDown();
            stop();
        }
    }

}
