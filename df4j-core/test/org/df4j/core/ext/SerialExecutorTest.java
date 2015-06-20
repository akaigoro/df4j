/*
 * Copyright 2011 by Alexei Kaigorodov
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.df4j.core.ext;

import java.util.Random;

import org.df4j.core.actor.Actor1;
import org.df4j.core.ext.SerialExecutor;
import org.df4j.core.Port;
import org.df4j.test.util.IntValue;
import org.df4j.test.util.MessageSink;
import org.junit.Assert;
import org.junit.Test;

/**
 * A set of identical Actors, passing packets to a randomly selected peer actor.
 * Unlike MultiPortActor, each actor has its own queue.
 * A packet dies after passing predefined number of hops.
 */
public class SerialExecutorTest {
    final static int NUM_ACTORS = 4; // number of nodes
    final static int NR_REQUESTS = NUM_ACTORS * 10; // 100; // number of tokens
    final static int TIME_TO_LIVE = 4; // hops
    final static int nThreads = Runtime.getRuntime().availableProcessors()*4;
    
    SerialExecutor serex=new SerialExecutor();
	volatile NodeActor activeNode;
	volatile boolean jam=false;
	
    /**
     * The intermediate passing node
     */
    class NodeActor extends Actor1<IntValue> {
        NodeActor[] nodes;
        private final Port<Object> sink;
        private Random rand;

        public NodeActor(long seed, NodeActor[] nodes, Port<Object> sink) {
            super(serex);
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
        	if (activeNode!=null) { // check that no other NodeActor is running now
        		jam=true;
        	}
        	activeNode=this;
            int nextVal = token.value - 1;
            if (nextVal > 0) {
                token.value = nextVal;
                NodeActor nextNode = nodes[rand.nextInt(nodes.length)];
                nextNode.post(token);
            } else {
                sink.post(token);
            }
            Thread.sleep(20);
        	if (activeNode!=this) {
        		jam=true;
        	}
        	activeNode=null;
        }
    }

    /**
     * checks that NodeActors never run in parallel
     * @throws InterruptedException
     */
    @Test
    public void runTest() throws InterruptedException {
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

    	Assert.assertFalse(jam);
    }

    public static void main(String args[]) throws InterruptedException {
        SerialExecutorTest nt = new SerialExecutorTest();
        nt.runTest();
    }

}
