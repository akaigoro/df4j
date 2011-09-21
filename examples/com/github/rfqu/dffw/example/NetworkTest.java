/*
 * Copyright 2011 by Alexei Kaigorodov
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package com.github.rfqu.dffw.example;

import java.io.PrintStream;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.Before;
import org.junit.Test;

import com.github.rfqu.dffw.core.*;


/**
 * A set of identical Actors, passing packets to a randomly selected peer actor.
 * A packet dies after passing predefined number of hops.
 *
 */
public class NetworkTest {
    final static int NUM_ACTORS = 100; // number of nodes
    final static int NR_REQUESTS = NUM_ACTORS * 10; // 100; // number of tokens
    final static int TIME_TO_LIVE = 1000; // hops
    final static int nThreads = 1;
    final static int times = 10;
    PrintStream out = System.out;

    @Before
    public void init() {
        out.println("Network with " + NUM_ACTORS + " nodes, " + NR_REQUESTS + " tokens, with " + TIME_TO_LIVE + " each, on " + nThreads + " threads");
    }

    public static void main(String args[]) throws InterruptedException {
        NetworkTest nt = new NetworkTest();
        nt.init();
        nt.test1();
        nt.test2();
    }

    @Test
    public void test1() throws InterruptedException {
        String workerName = SimpleExecutorService.class.getCanonicalName();
        out.println("Using " + workerName);
        if (nThreads > 1) {
            out.println(" warning: " + workerName + " uses only 1 thread");
        }
        SimpleExecutorService executor = new SimpleExecutorService();
        for (int i = 0; i < times; i++) {
            runNetwork(executor);
        }
        executor.shutdown();
    }

    @Test
    public void test2() throws InterruptedException {
        out.println("Using " + ExecutorService.class.getCanonicalName());
        ThreadFactoryTL tf = new ThreadFactoryTL();
        ExecutorService executor = Executors.newFixedThreadPool(nThreads, tf);
        tf.setExecutor(executor);
        for (int i = 0; i < times; i++) {
            runNetwork(executor);
        }
        executor.shutdown();
    }

    /**
     * the type of messages floating between nodes
     */
    static class Packet extends Link {
        int hops_remained;

        public Packet(int hops_remained) {
            this.hops_remained = hops_remained;
        }
    }

    /**
     * the initial node of the network. This is Runnable rather than Actor.
     */
    static class Starter implements Runnable {
        MessageSink sink = new MessageSink(NR_REQUESTS);
        NodeActor[] nodes = new NodeActor[NUM_ACTORS];
        Random rand = new Random(1);

        @Override
        public void run() {
            // create passing nodes
            for (int i = 0; i < nodes.length; i++) {
                nodes[i] = new NodeActor(rand.nextLong(), nodes, sink);
            }
            // send packets to random nodes
            for (int k = 0; k < NR_REQUESTS; k++) {
                Actor<Packet> nextInbox = nodes[rand.nextInt(nodes.length)];
                nextInbox.send(new Packet(TIME_TO_LIVE));
            }
        }
    }

    /**
     * The intermediate passing node
     * 
     */
    static class NodeActor extends Actor<Packet> {
        NodeActor[] nodes;
        private final Port<Packet> sink;
        private Random rand;

        public NodeActor(long seed, NodeActor[] nodes, Port<Packet> sink) {
            this.nodes = nodes;
            this.sink = sink;
            this.rand = new Random(seed);
        }

        /**
         * the method to handle incoming messages for each received packet,
         * decrease the number of remaining hops. If number of hops become zero,
         * send it to sink, otherwise send to another node.
         */
        @Override
        protected void act(Packet token) throws Exception {
            int nextVal = token.hops_remained - 1;
            if (nextVal == 0) {
                sink.send(token);
            } else {
                token.hops_remained = nextVal;
                Port<Packet> nextNode = nodes[rand.nextInt(nodes.length)];
                nextNode.send(token);
            }
        }
    }

    /**
     * The ending node. This is Port rather than actor, to be accessed outside
     * the actor world.
     */
    static class MessageSink extends CountDownLatch implements Port<Packet> {

        public MessageSink(int count) {
            super(count);
        }

        @Override
        public Object send(Packet message) {
            super.countDown();
            return this;
        }

    }

    /**
     * the core of the test
     */
    float runNetwork(Executor executor) throws InterruptedException {
        long startTime = System.currentTimeMillis();

        // actors can only be accessed from threads which carry executor as a
        // thread local variable,
        // so initial packets are send from within the executor using Runnable.
        Starter starter = new Starter();
        executor.execute(starter);

        // wait for all packets to die.
        starter.sink.await();

        // report timings
        long etime = (System.currentTimeMillis() - startTime);
        float switchnum = NR_REQUESTS * ((long) TIME_TO_LIVE);
        float delay = etime * 1000 * nThreads / switchnum;
        out.println("Elapsed=" + etime / 1000f + " sec; rate=" + (1 / delay) + " messages/mks/core; mean hop time=" + (delay * 1000) + " ns");
        return delay;
    }

}
