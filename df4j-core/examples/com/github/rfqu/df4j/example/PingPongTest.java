/*
 * Copyright 2011 by Alexei Kaigorodov
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package com.github.rfqu.df4j.example;

import java.io.PrintStream;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.github.rfqu.df4j.core.*;


/**
 * A set of identical Ping Actors, passing tokens to a single Pong actor, which
 * returns tokens back to sender. A tokens dies after predefined number of hops.
 * 
 */
public class PingPongTest {
    final static int NUM_ACTORS = 100; // number of nodes
    final static int NUM_TOKENS = NUM_ACTORS * 10; // number of tokens
    final static int TIME_TO_LIVE = 1000; // hops
    final static int nThreads = Runtime.getRuntime().availableProcessors();
    final static int times = 5;
    PrintStream out = System.out;
    ExecutorService executor;
    
    @Before
    public void init() {
        out.println("Network with " + NUM_ACTORS + " nodes, " + NUM_TOKENS + " tokens, with " + TIME_TO_LIVE + " each, on " + nThreads + " threads");
    }
    
    @After
    public void shutdown() {
        executor.shutdown();
    }

    @Test
    public void testS() throws InterruptedException {
        executor = new SimpleExecutorService();
        test();
    }

    @Test
    public void testJUC() throws InterruptedException {
        ThreadFactoryTL tf = new ThreadFactoryTL();
        executor = Executors.newFixedThreadPool(nThreads, tf);
        tf.setExecutor(executor);
        test();
    }

    public void test() throws InterruptedException {
        Task.setCurrentExecutor(executor);
        out.println("Using " + executor.getClass().getSimpleName());
        for (int i = 0; i < times; i++) {
            runPingPong();
        }
    }

    /**
     * the core of the test
     */
    float runPingPong() throws InterruptedException {
        Random rand = new Random(1);
        Ping[] pings = new Ping[NUM_ACTORS];
        MessageSink sink = new MessageSink(NUM_TOKENS);
        // create Pong actor
        Pong pong = new Pong(sink);
        // create Ping actors
        for (int i = 0; i < pings.length; i++) {
            pings[i] = new Ping(pong, sink);
        }

        long startTime = System.currentTimeMillis();
        // create packets, send them to randomly chosen actors
        for (int i = 0; i < NUM_TOKENS; i++) {
            Ping ping = pings[rand.nextInt(pings.length)];
            Token packet = new Token(TIME_TO_LIVE);
            ping.send(packet);
        }

        // wait for all packets to die.
        sink.await();

        // report timings
        long etime = (System.currentTimeMillis() - startTime);
        float switchnum = NUM_TOKENS * ((long) TIME_TO_LIVE);
        float delay = etime * 1000 * nThreads / switchnum;
        out.println("Elapsed=" + etime / 1000f + " sec; rate=" + (1 / delay) + " messages/mks/core; mean hop time=" + (delay * 1000) + " ns");
        return delay;
    }

    /**
     * the type of messages floating between nodes
     */
    static class Token extends Request<Token> {
        int hops_remained;

        public Token(int hops_remained) {
            this.hops_remained = hops_remained;
        }
    }

    /**
     * The pinging actor
     * 
     */
    static class Ping extends Actor<Token> {
        Pong pong;
        Port<Token> sink;

        public Ping(Pong pong, Port<Token> sink) {
            this.pong = pong;
            this.sink = sink;
        }

        /**
         * Handle incoming messages. For each received packet, decrease the
         * number of remaining hops. If number of hops become zero, send it to
         * sink, otherwise send to the Pong actor.
         */
        @Override
        protected void act(Token token) throws Exception {
            int nextVal = token.hops_remained - 1;
            if (nextVal == 0) {
                sink.send(token);
            } else {
                token.hops_remained = nextVal;
                pong.request(token, this);
            }
        }
    }

    /**
     * The ponging actor
     * 
     */
    static class Pong extends Actor<Token> {
        private final Port<Token> sink;

        public Pong(Port<Token> sink) {
            this.sink = sink;
        }

        
        public void request(Token token, Port<Token> callback) {
            token.callback = callback;
            send(token);
        }
        
        /**
         * the method to handle incoming messages for each received packet,
         * decrease the number of remaining hops. If number of hops become zero,
         * send it to sink, otherwise send it back to the Ping actor.
         */
        @Override
        protected void act(Token token) throws Exception {
            int nextVal = token.hops_remained - 1;
            if (nextVal == 0) {
                sink.send(token);
            } else {
                token.hops_remained = nextVal;
                token.reply(token);
            }
        }
    }

    /**
     * The ending node. This is a Port rather than actor.
     */
    static class MessageSink extends CountDownLatch implements Port<Token> {

        public MessageSink(int count) {
            super(count);
        }

        @Override
        public MessageSink send(Token message) {
            super.countDown();
            return this;
        }

    }

    public static void main(String args[]) throws InterruptedException {
        PingPongTest nt = new PingPongTest();
        nt.init();
        nt.testS();
        nt.testJUC();
    }

}
