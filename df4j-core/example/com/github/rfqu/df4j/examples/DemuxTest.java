/*
 * Copyright 2011 by Alexei Kaigorodov
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package com.github.rfqu.df4j.examples;

import java.io.PrintStream;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;

import org.junit.Before;
import org.junit.Test;

import com.github.rfqu.df4j.core.Actor;
import com.github.rfqu.df4j.core.AbstractActor;
import com.github.rfqu.df4j.core.Link;
import com.github.rfqu.df4j.core.Port;
import com.github.rfqu.df4j.core.PullingActor;
import com.github.rfqu.df4j.core.Request;
import com.github.rfqu.df4j.core.StreamActor;
import com.github.rfqu.df4j.core.StreamPort;
import com.github.rfqu.df4j.core.Task;
import com.github.rfqu.df4j.core.ThreadFactoryTL;

/**
 * In multithreaded programming, often several identical working threads are fed with
 * a single input queue. If we want to replace threads with actor, this cannot be done
 * directly, as actors may not be blocked (when the queue is empty). 
 * This sample code shows how to build a demultiplexer to feed several actors with single queue.
 * Actors work in parallel. 
 * The actor wanting to be fed sends itself to the actors port with Demux.listen(this).
 */
public class DemuxTest {
    
    static class Demux<M extends Link> extends AbstractActor implements StreamPort<M> {
        protected final StreamInput<M> input=new StreamInput<M>();
        protected final StreamInput<StreamActor<M>> actors=new StreamInput<StreamActor<M>>();
        
        protected Demux() {
            super(null);
        }

        public void listen(Actor<M> actor) {
            actors.send(actor);
        }

        @Override
        public void send(M token) {
            input.send(token);
        }

        @Override
        public void close() {
            input.close();
        }
        
        @Override
        protected void act() {
            StreamActor<M> actor = actors.value;
            M value = input.value;
            if (value==null) {
                // input closed
                actor.close();
            } else {
                actor.send(value);
            }
        }

    }
    
    final static int NUM_ACTORS = 1000; // number of Ping nodes
    final static int NUM_TOKENS = NUM_ACTORS; // number of tokens
    final static int TIME_TO_LIVE = 1000; // hops
    final static int times = 5;
    int nThreads;
    PrintStream out = System.out;

    @Before
    public void init() {
        out.println("Network with " + NUM_ACTORS + " nodes, " + NUM_TOKENS + " tokens, with " + TIME_TO_LIVE + " each, on " + nThreads + " threads");
    }

    @Test
    public void testSingle() throws InterruptedException {
        nThreads=1;
        runTest(ThreadFactoryTL.newSingleThreadExecutor());
    }

    @Test
    public void testFixed() throws InterruptedException {
        nThreads= Runtime.getRuntime().availableProcessors();
        runTest(ThreadFactoryTL.newFixedThreadPool(nThreads));
    }

    private void runTest(Executor executor) throws InterruptedException {
        String workerName = executor.getClass().getCanonicalName();
        out.println("Using " + workerName);
        Task.setCurrentExecutor(executor);
		for (int i = 0; i < times; i++) {
            runPingPong();
        }
	}

    /**
     * the type of messages floating between nodes
     */
    static class Token extends Request<Token, Void> {
        private int hops_remained;
        private final Port<Token> sink;

        public Token(int hops_remained, Port<Token> sink) {
            this.hops_remained = hops_remained;
            this.sink = sink;
        }
        
        /**
         * the method to handle incoming messages for each received packet,
         * decrease the number of remaining hops. If number of hops become zero,
         * send it to sink, otherwise send it back to the Ping actor.
         */
        protected void sendTo(Port<Token> to) throws Exception {
            int nextVal = hops_remained - 1;
            if (nextVal == 0) {
                sink.send(this);
            } else {
                hops_remained = nextVal;
                to.send(this);
            }
        }
    }
    
    /**
     * The pinging actor
     * 
     */
    static class Ping extends Actor<Token> {
        Port<Token> pong;

        public Ping(Port<Token> pong) {
            this.pong = pong;
        }

        /**
         * Handle incoming messages. For each received packet, decrease the
         * number of remaining hops. If number of hops become zero, send it to
         * sink, otherwise send to the Pong actor.
         */
        protected void act(Token token) throws Exception {
            token.setReplyTo(this);
            token.sendTo(pong);
        }
    }


    static class Pong extends Demux<Token> {
        
        /**
         * The ponging actor
         * 
         */
        class PongWorker extends PullingActor<Token> {
            {
                actors.send(this);
            }

            @Override
            protected void act(Token token) throws Exception {
                token.sendTo(token.getReplyTo());
                actors.send(this);
            }
        }
    }
    
    /**
     * The ending node. This is a Port rather than actor, to be accessed outside
     * the actor world.
     */
    static class MessageSink extends CountDownLatch implements Port<Token> {
        int remain;

        public MessageSink(int count) {
            super(count);
            remain = count;
        }

        @Override
        public void send(Token message) {
            super.countDown();
            remain--;
        }

    }

    /**
     * the core of the test
     */
    float runPingPong() throws InterruptedException {
        long startTime = System.currentTimeMillis();

        MessageSink sink = new MessageSink(NUM_TOKENS);
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
            pings[rand.nextInt(pings.length)].send(new Token(TIME_TO_LIVE, sink));
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

    public static void main(String args[]) throws InterruptedException {
        DemuxTest nt = new DemuxTest();
        nt.init();
        nt.testSingle();
    }

}
