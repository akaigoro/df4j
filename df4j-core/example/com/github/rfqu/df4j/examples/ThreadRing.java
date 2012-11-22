/*
 * Copyright 2012 by Alexei Kaigorodov
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package com.github.rfqu.df4j.examples;
import java.io.PrintStream;
import java.util.concurrent.Executor;

import org.junit.Test;

import com.github.rfqu.df4j.core.ContextThreadFactory;
import com.github.rfqu.df4j.core.Port;
import com.github.rfqu.df4j.core.Request;
import com.github.rfqu.df4j.core.Task;
import com.github.rfqu.df4j.ext.ActorLQ;
import com.github.rfqu.df4j.ext.ImmediateExecutor;
import com.github.rfqu.df4j.testutil.MessageSink;

/**
 * see http://shootout.alioth.debian.org/u32/performance.php?test=threadring
 * @author rfq
 *
 */
public class ThreadRing {
    final static int NUM_ACTORS = 503; // number of nodes
    final static int TIME_TO_LIVE = 100000; // hops
    final static PrintStream out = System.out;
    final static int times = 3;
    int nThreads;
    
    @Test
    public void testImm() throws InterruptedException  {
        nThreads=1;
        runTest(new ImmediateExecutor());
    }

    @Test
    public void testSingle() throws InterruptedException  {
        nThreads=1;
        runTest(ContextThreadFactory.newSingleThreadExecutor());
    }

    @Test
    public void testFixed() throws InterruptedException {
        nThreads= Runtime.getRuntime().availableProcessors();
        runTest(ContextThreadFactory.newFixedThreadPool(nThreads));
    }

    private void runTest(Executor executor) throws InterruptedException {
        out.println("Ring with " + NUM_ACTORS + " nodes, with " + TIME_TO_LIVE + " each, on " + nThreads + " threads");
        String workerName = executor.getClass().getCanonicalName();
        out.println("Using " + workerName);
        Task.setCurrentExecutor(executor);
        for (int i = 0; i < times; i++) {
            runNetwork();
        }
    }
    
    /**
     * the core of the test
     */
    float runNetwork() throws InterruptedException {
        long startTime = System.currentTimeMillis();

        MessageSink<Object> sink = new MessageSink<Object>(1);
        NodeActor[] nodes = new NodeActor[NUM_ACTORS];

        // create passing nodes
        for (int i = 0; i < nodes.length; i++) {
            nodes[i] = new NodeActor(i,sink);
        }
        // connect passing nodes
        for (int i = 0; i < nodes.length; i++) {
            nodes[i].setNext(nodes[(i+1)%nodes.length]);
        }
        // send packets to random nodes
        nodes[0].send(new Token(TIME_TO_LIVE));

        // wait for all packets to die.
        sink.await();

        // report timings
        long etime = (System.currentTimeMillis() - startTime);
        float switchnum = ((float) TIME_TO_LIVE);
        float delay = etime * 1000 * nThreads / switchnum;
        out.println("Elapsed=" + etime / 1000f + 
                " sec; rate=" + (1 / delay) + 
                " messages/mks/core/us; mean hop time=" + 
                delay + " us");
        return delay;
    }

    /**
     * The intermediate passing node
     * 
     */
    static class NodeActor extends ActorLQ<Token> {
        private final Port<Object> sink;
        private NodeActor next;
        int id;

        public NodeActor(int id, Port<Object> sink) {
            this.id = id;
            this.sink = sink;
        }

        public void setNext(NodeActor next) {
            this.next = next;
        }

        /**
         * the method to handle incoming messages for each received packet,
         * decrease the number of remaining hops. If number of hops become zero,
         * send it to sink, otherwise send to another node.
         */
        @Override
        protected void act(Token token) throws Exception {
            int nextVal = token.hops_remained - 1;
            if (nextVal == 0) {
                sink.send(token);
                System.out.println(""+id);
            } else {
                token.hops_remained = nextVal;
                next.send(token);
            }
        }
    }

    /**
     * the type of messages floating between nodes
     */
    static class Token extends Request<Token, Void> {
        int hops_remained;

        public Token(int hops_remained) {
            this.hops_remained = hops_remained;
        }
    }
}
