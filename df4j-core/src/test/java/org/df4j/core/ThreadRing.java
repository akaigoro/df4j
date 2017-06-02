/*
 * Copyright 2012 by Alexei Kaigorodov
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.df4j.core;
import java.io.IOException;
import java.io.PrintStream;
import org.df4j.test.util.MessageSink;
import org.junit.Test;

/**
 * see http://benchmarksgame.alioth.debian.org/u64q/threadring-description.html#threadring
 * @author rfq
 *
 */
public class ThreadRing {
    final static int NUM_ACTORS = 503; // number of nodes
    final static int TIME_TO_LIVE = 1000000; // hops
    final static PrintStream out = System.out;
    final static int times = 4;
    
    @Test
    public void runTest() throws InterruptedException {
        out.println("* Ring with " + NUM_ACTORS + " nodes, with " + TIME_TO_LIVE + " each");
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
        nodes[0].post(new Token(TIME_TO_LIVE));

        // wait for all packets to die.
        sink.await();

        // report timings
        long etime = (System.currentTimeMillis() - startTime);
        float switchnum = ((float) TIME_TO_LIVE);
        int nThreads=Runtime.getRuntime().availableProcessors();
        float delay = etime * 1000 * nThreads / switchnum;
        out.println("Elapsed=" + etime / 1000f + 
                " sec; rate=" + (1 / delay) + 
                " messages/mks/core/us; mean hop time=" + 
                delay + " us");
        return delay;
    }

    static class Token {//extends Request<Token, Void> {
        public int hops_remained;

        public Token(int hops_remained) {
            this.hops_remained = hops_remained;
        }
    }

    /**
     * The intermediate passing node
     * 
     */
    static class NodeActor extends Actor1<Token> {
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
                sink.post(token);
            } else {
                token.hops_remained = nextVal;
                next.post(token);
            }
        }
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        ThreadRing t= new ThreadRing();
//        System.in.read();
        t.runTest();
    }
}
