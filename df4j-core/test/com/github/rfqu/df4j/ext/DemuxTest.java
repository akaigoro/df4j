/*
 * Copyright 2011 by Alexei Kaigorodov
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package com.github.rfqu.df4j.ext;

import java.io.PrintStream;

import org.junit.Test;

import com.github.rfqu.df4j.core.Actor;
import com.github.rfqu.df4j.ext.Demux;
import com.github.rfqu.df4j.util.IntValue;

/**
 * In multithreaded programming, often several identical working threads are fed with
 * a single input queue. If we want to replace threads with actor, this cannot be done
 * directly, as actors may not be blocked (when the queue is empty). 
 * This sample code shows how to build a demultiplexer to feed several actors with single queue.
 * Actors work in parallel. 
 * The actor wanting to be fed sends itself to the actors port with Demux.listen(this).
 */
public class DemuxTest {
    int nThreads;
    PrintStream out = System.out;

    @Test
    public void test1() throws InterruptedException {
        Pong pong=new Pong();
        for (int k=0; k<100; k++) {
            pong.send(new IntValue(k));
        }
	}

    static class Pong extends Demux<IntValue> {
        { 
            for (int k=0; k<10; k++) {
                new PongWorker(k);
            }
        }
        
        /**
         * The ponging actor
         * 
         */
        class PongWorker extends Actor<IntValue> {
            int id;
            {
                actors.send(this);
            }

            public PongWorker(int id) {
                this.id=id;
            }

            @Override
            protected void act(IntValue message) throws Exception {
                System.out.println("actor:"+id+" m:"+message.value);
                System.out.flush();
                actors.send(this);
            }
        }
    }

    public static void main(String args[]) throws InterruptedException {
        DemuxTest nt = new DemuxTest();
        nt.test1();
    }

}
