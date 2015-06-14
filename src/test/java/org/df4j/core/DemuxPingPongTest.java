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

import java.io.PrintStream;
import java.util.Random;
import org.df4j.core.Actor1;
import org.df4j.core.ext.Dispatcher;
import org.df4j.core.Port;
import org.df4j.test.util.MessageSink;
import org.df4j.test.util.Packet;
import org.junit.Test;

public class DemuxPingPongTest {

    final static int NUM_ACTORS = 1000; // number of Ping nodes
    final static int NUM_TOKENS = NUM_ACTORS; // number of tokens
    final static int TIME_TO_LIVE = 1000; // hops
    final static int times = 5;
    PrintStream out = System.out;

    @Test
    public void runTest() throws InterruptedException {
        out.println("* Network with " + NUM_ACTORS + " nodes, " + NUM_TOKENS
        		+ " tokens, with " + TIME_TO_LIVE + " hops");
        for (int i = 0; i < times; i++) {
            runPingPong();
        }
    }

    /**
     * the core of the test
     */
    float runPingPong() throws InterruptedException {
        long startTime = System.currentTimeMillis();

        MessageSink<Packet> sink = new MessageSink<Packet>(NUM_TOKENS);
        Ping[] pings = new Ping[NUM_ACTORS];
        Random rand = new Random(1);

        // create Pong actor
        Pong pong = new Pong();
        // create Ping actors
        for (int i = 0; i < pings.length; i++) {
            pings[i] = new Ping(pong);
        }
        // create tokens, send them to randomly chosen Ping actors
        for (int i = 0; i < NUM_TOKENS; i++) {
            pings[rand.nextInt(pings.length)].post(new Packet(TIME_TO_LIVE, sink));
        }

        // wait for all packets to die.
        sink.await();

        // report timings
        long etime = (System.currentTimeMillis() - startTime);
        float switchnum = NUM_TOKENS * ((long) TIME_TO_LIVE);
//        int nThreads=ForkJoinPool.commonPool().getActiveThreadCount(); --- gives 0
        int nThreads=Runtime.getRuntime().availableProcessors();
        float delay = etime * 1000 * nThreads / switchnum;
        out.println("Elapsed=" + etime / 1000f + " sec; rate=" + (1 / delay)
        		+ " messages/mks/core; mean hop time=" + (delay * 1000) + " ns"
        		+  " on " + nThreads + " threads");
        return delay;
    }

    /**
     * The pinging actor
     * 
     */
    static class Ping extends Actor1<Packet> {
        Port<Packet> pong;

        public Ping(Port<Packet> pong) {
            this.pong = pong;
        }

        /**
         * Handle incoming messages. For each received packet, decrease the number of remaining hops. If number of hops become zero, send it to sink, otherwise send to the Pong
         * actor.
         */
        protected void act(Packet token) throws Exception {
            token.send(this, pong);
        }
    }

    static class Pong extends Dispatcher<Packet> {
        {
            for (int k = 0; k < 3; k++) {
                new PongWorker();
            }
        }

        /**
         * The ponging actor
         * 
         */
        class PongWorker extends Actor1<Packet> {
            {
                listen(this);
            }

            @Override
            protected void act(Packet token) throws Exception {
                token.reply();
                listen(this);
            }
        }
    }

    public static void main(String args[]) throws InterruptedException {
        DemuxPingPongTest nt = new DemuxPingPongTest();
        nt.runTest();
    }

}
