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
import java.util.concurrent.ExecutorService;

import org.junit.Before;
import org.junit.Test;

import com.github.rfqu.df4j.core.*;
import com.github.rfqu.df4j.util.MessageSink;

class Packet extends Link  {
    int hops_remained;

    public Packet(int hops_remained) {
        this.hops_remained = hops_remained;
    }
}

class Graph extends AbstractDemux<String, Packet, Graph.NodeActor> {
    MessageSink sink = new MessageSink(DemuxGraphTest.NR_REQUESTS);
    Random rand = new Random(1);

	@Override
	protected AbstractDelegator<Packet, NodeActor> createDelegator(String tag) {
		return new ConservativeDelegator<Packet, NodeActor>();
	}

	@Override
	protected void requestHandler(String tag, AbstractDelegator<Packet, NodeActor> gate) {
        gate.setHandler(new NodeActor());
    }
    
    /**
     * Intermediate passing node
     */
    class NodeActor implements Delegate<Packet> {
		@Override
		public void act(Packet p) throws Exception {
	        int nextVal = p.hops_remained - 1;
	        if (nextVal == 0) {
	            sink.send(p);
	        } else {
	            p.hops_remained = nextVal;
	        	toRandomNode(p);
	        }
		}

		@Override
		public void complete() throws Exception {
		}
    }

    void toRandomNode(Packet p) {
        String tag = Long.toHexString(rand.nextInt(DemuxGraphTest.NUM_ACTORS));
        send(tag, p);
    }

    /**
     * the core of the test
     */
    float runNetwork() throws InterruptedException {
        long startTime = System.currentTimeMillis();

        // send packets to random nodes
        for (int k = 0; k < DemuxGraphTest.NR_REQUESTS; k++) {
            toRandomNode(new Packet(DemuxGraphTest.TIME_TO_LIVE));
        }

        // wait for all packets to die.
        sink.await();

        // report timings
        long etime = (System.currentTimeMillis() - startTime);
        float switchnum = DemuxGraphTest.NR_REQUESTS * ((long) DemuxGraphTest.TIME_TO_LIVE);
        float delay = etime * 1000 * DemuxGraphTest.nThreads / switchnum;
        DemuxGraphTest.out.println("Elapsed=" + etime / 1000f + " sec; rate=" + (1 / delay) + " messages/mks/core; mean hop time=" + (delay * 1000) + " ns");
        return delay;
    }

}

/**
 * A set of identical Actors, passing packets to a randomly selected peer actor.
 * A packet dies after passing predefined number of hops.
 *
 */
public class DemuxGraphTest {
    final static int NUM_ACTORS = 100; // number of nodes
    final static int NR_REQUESTS = NUM_ACTORS * 10; // 100; // number of tokens
    final static int TIME_TO_LIVE = 1000; // hops
    final static int nThreads = Runtime.getRuntime().availableProcessors();
    final static int times = 10;
    static PrintStream out = System.out;

    @Before
    public void init() {
        out.println("Graph with " + NUM_ACTORS + " nodes, " + NR_REQUESTS + " tokens, with " + TIME_TO_LIVE + " each, on " + nThreads + " threads");
    }

    @Test
    public void testSimple() throws InterruptedException {
        runTest(new SimpleExecutorService());
    }

    @Test
    public void testFixed() throws InterruptedException {
        runTest(ThreadFactoryTL.newFixedThreadPool(nThreads));
    }

	private void runTest(ExecutorService executor) throws InterruptedException {
        out.println("Using " + executor.getClass().getCanonicalName());
		Task.setCurrentExecutor(executor);
        for (int i = 0; i < times; i++) {
            new Graph().runNetwork();
        }
        executor.shutdown();
	}


    public static void main(String args[]) throws InterruptedException {
        DemuxGraphTest nt = new DemuxGraphTest();
        nt.init();
        nt.testSimple();
        nt.testFixed();
    }

}
