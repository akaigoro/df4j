/*
 * Copyright 2011 by Alexei Kaigorodov
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.df4j.core;

import org.df4j.core.ext.Actor1;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertTrue;

/**
 * A set of identical PingPong Actors, passing tokens to a single Pong actor, which
 * returns tokens back to sender. A tokens dies after predefined number of hops.
 * 
 */
public class PingPongTest {

    @Test
    public void runTest() throws InterruptedException {
        PingPong ping = new PingPong();
        PingPong pong = new PingPong();
        CountDownLatch sink = new CountDownLatch(1);
        int hops_remained = 2000000;
        Ball ball = new Ball(hops_remained, sink);
        long start = System.currentTimeMillis();
        ball.send(ping, pong);
        assertTrue(sink.await(10000, TimeUnit.MILLISECONDS));
        long period = System.currentTimeMillis() - start;
        double hopTime = period * 1000000.0 / hops_remained;
        System.out.printf("period = %d ms; Mean hop time = %f ns", period, hopTime);
    }

    /**
     * the type of messages floating between nodes
     */
    static class Ball {
        int hops_remained;
        CountDownLatch sink;
        Port<Ball> sender;

        public Ball(int hops_remained, CountDownLatch sink) {
            this.hops_remained = hops_remained;
            this.sink = sink;
        }

        public void send(Port<Ball> from, Port<Ball> to) {
            sender=from;
            to.post(this);
        }

        public void reflect(Port<Ball> from) {
            if (--hops_remained <= 0) {
                sink.countDown();
            } else {
                send(from, sender);
            }
        }
    }

    /**
     * The pinging actor
     *
     */
    static class PingPong extends Actor1<Ball> {

        /**
         * Handle incoming messages. For each received packet, decrease the
         * number of remaining hops. If number of hops become zero, send it to
         * sink, otherwise send to the Pong actor.
         */
        protected void act(Ball token) throws Exception {
            token.reflect(this);
        }
    }
}
