/*
 * Copyright 2011 by Alexei Kaigorodov
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.df4j.core.actor;

import java.io.PrintStream;
import java.util.Random;

import org.df4j.core.DFContext;
import org.df4j.core.actor.Actor1;
import org.df4j.core.ext.ImmediateExecutor;
import org.df4j.test.util.MessageSink;
import org.df4j.test.util.Packet;
import org.junit.Test;

/**
 * A set of identical Ping Actors, passing tokens to a single Pong actor, which
 * returns tokens back to sender. A tokens dies after predefined number of hops.
 * 
 */
public class PingPongTest {
    final static int NUM_ACTORS = 1000; // number of Ping nodes
    final static int NUM_TOKENS = NUM_ACTORS; // number of tokens
    final static int TIME_TO_LIVE = 1000; // hops
    final static int times = 5;
    int nThreads;
    PrintStream out = System.out;

    @Test
    public void testImm() throws InterruptedException  {
        nThreads=1;
        DFContext.setCurrentExecutor(new ImmediateExecutor());
		runTest();
    }

    @Test
    public void testSingle() throws InterruptedException {
        nThreads=1;
        DFContext.setSingleThreadExecutor();
        runTest();
    }

    @Test
    public void testFixed() throws InterruptedException {
        nThreads= Runtime.getRuntime().availableProcessors();
        DFContext.setFixedThreadPool(nThreads);
        runTest();
    }

    private void runTest() throws InterruptedException {
        String workerName = DFContext.getCurrentExecutor().getClass().getCanonicalName();
        out.println("Network with " + NUM_ACTORS + " nodes, " + NUM_TOKENS + " tokens, with " + TIME_TO_LIVE + " each, on " + nThreads + " threads");
        out.println("Using " + workerName);
		for (int i = 0; i < times; i++) {
            runPingPong();
        }
	}

    /**
     * The pinging actor
     * 
     */
    static class Ping extends Actor1<Packet> {
        Pong pong;

        public Ping(Pong pong) {
            this.pong = pong;
        }

        /**
         * Handle incoming messages. For each received packet, decrease the
         * number of remaining hops. If number of hops become zero, send it to
         * sink, otherwise send to the Pong actor.
         */
        protected void act(Packet token) throws Exception {
            token.send(this, pong);
        }
    }

    /**
     * The ponging actor
     * 
     */
    static class Pong extends Actor1<Packet> {

        /**
         * the method to handle incoming messages for each received packet,
         * decrease the number of remaining hops. If number of hops become zero,
         * send it to sink, otherwise send it back to the Ping actor.
         */
        @Override
        protected void act(Packet token) throws Exception {
            token.reply();
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
        // create tokens, send them to randomly chosen actors
        for (int i = 0; i < NUM_TOKENS; i++) {
            pings[rand.nextInt(pings.length)].post(new Packet(TIME_TO_LIVE, sink));
        }

        // wait for all packets to die.
        sink.await();

        // report timings
        long etime = (System.currentTimeMillis() - startTime);
        float switchnum = NUM_TOKENS * ((long) TIME_TO_LIVE);
        float delay = etime * 1000 * nThreads / switchnum;
        out.println("Elapsed=" + etime / 1000f
                + " sec; rate=" + (1 / delay)
                + " messages/mks/core; mean hop time="
                + (delay * 1000) + " ns");
        return delay;
    }

    public static void main(String args[]) throws InterruptedException {
        PingPongTest nt = new PingPongTest();
        nt.testSingle();
    }

}
