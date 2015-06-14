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
import org.df4j.test.util.IntValue;
import org.df4j.test.util.MessageSink;
import org.junit.Test;

/**
 * A set of identical Actors, passing packets to a randomly selected peer actor.
 * A packet dies after passing predefined number of hops.
 *
 */
public class GraphTest {
    final static int NUM_ACTORS = 100; // number of nodes
    final static int NR_REQUESTS = NUM_ACTORS * 10; // 100; // number of tokens
    final static int TIME_TO_LIVE = 1000; // hops
    final static int times = 8;
    final static PrintStream out = System.out;

    @Test
    public void runTest() throws InterruptedException {
        out.println("Graph with " + NUM_ACTORS +
                " nodes, " + NR_REQUESTS + 
                " tokens, with " + TIME_TO_LIVE + " each");
        for (int i = 0; i < times; i++) {
            runNetwork();
        }
	}

    /**
     * The intermediate passing node
     * 
     */
    static class NodeActor extends Actor1<IntValue> {
        NodeActor[] nodes;
        private final Port<Object> sink;
        private Random rand;

        public NodeActor(long seed, NodeActor[] nodes, Port<Object> sink) {
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
        protected void act(IntValue token) throws Exception {
            int nextVal = token.value - 1;
            if (nextVal == 0) {
                sink.post(token);
            } else {
                token.value = nextVal;
                NodeActor nextNode = nodes[rand.nextInt(nodes.length)];
                nextNode.post(token);
            }
        }
    }

    /**
     * the core of the test
     */
    float runNetwork() throws InterruptedException {
        long startTime = System.currentTimeMillis();

        MessageSink<Object> sink = new MessageSink<Object>(NR_REQUESTS);
        NodeActor[] nodes = new NodeActor[NUM_ACTORS];
        Random rand = new Random(1);

        // create passing nodes
        for (int i = 0; i < nodes.length; i++) {
            nodes[i] = new NodeActor(rand.nextLong(), nodes, sink);
        }
        // send packets to random nodes
        for (int k = 0; k < NR_REQUESTS; k++) {
            Actor1<IntValue> nextInbox = nodes[rand.nextInt(nodes.length)];
            nextInbox.post(new IntValue(TIME_TO_LIVE));
        }

        // wait for all packets to die.
        sink.await();

        // report timings
        long etime = (System.currentTimeMillis() - startTime);
        float switchnum = NR_REQUESTS * ((long) TIME_TO_LIVE);
        int nThreads=Runtime.getRuntime().availableProcessors();
        float delay = etime * 1000 * nThreads / switchnum;
        out.println("Elapsed=" + etime / 1000f + 
                " sec; rate=" + (1 / delay) + 
                " messages/mks/core/us; mean hop time=" + 
                delay + " us");
        return delay;
    }

    public static void main(String args[]) throws InterruptedException {
        GraphTest nt = new GraphTest();
        nt.runTest();
    }

}
