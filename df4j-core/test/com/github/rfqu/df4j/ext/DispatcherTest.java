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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.PrintStream;
import java.util.concurrent.LinkedBlockingQueue;

import org.junit.Test;

import com.github.rfqu.df4j.core.Actor;
import com.github.rfqu.df4j.testutil.IntValue;

/**
 * In multithreaded programming, often several identical working threads are fed with
 * a single input queue. If we want to replace threads with actor, this cannot be done
 * directly, as actors may not be blocked (when the queue is empty). 
 * This sample code shows how to build a demultiplexer to feed several actors with single queue.
 * Actors work in parallel. 
 * The actor wanting to be fed sends itself to the actors port with Demux.listen(this).
 */
public class DispatcherTest {
    int nThreads;
    PrintStream out = System.out;

    @Test
    public void test1() throws InterruptedException {
        LinkedBlockingQueue<Token> q=new LinkedBlockingQueue<Token>();
        Pong pong=new Pong(1, q, true);
        pong.post(new Token(0));
    }

    /* no tokens */
    @Test
    public void test01() throws InterruptedException {
        testN(0, 1, true);
    }

    /* no tokens */
    @Test
    public void test02() throws InterruptedException {
        testN(0, 2, true);
    }

    /** checks that all sent tokens are processed
     */
    @Test
    public void test11() throws InterruptedException {
        testN(1, 1, true);
    }

    /** checks that all sent tokens are processed
     */
    @Test
    public void test23() throws InterruptedException {
        testN(2, 3, true);
    }

    /** checks that all sent tokens are processed
     */
    @Test
    public void testMany() throws InterruptedException {
        testN(1000, 100, false);
    }

    /**
     * various numbers of tokens and workers 
     * @param nt number of tokens
     * @param nw number of workers
     * @throws InterruptedException
     */
    public void testN(int nt, int nw, boolean print) throws InterruptedException {
        LinkedBlockingQueue<Token> q=new LinkedBlockingQueue<Token>();
        Pong pong=new Pong(nw, q, print);
        Token[] tokens=new Token[nt];
        for (int k=0; k<nt; k++) {
            final Token token = new Token(k);
            pong.post(token);
            tokens[k]=token;
        }
        for (int k=0; k<nt; k++) {
            final Token token = q.take();
            if (!token.touched) {
                System.out.println(token);
            }
            assertTrue(token.touched);
            assertNotNull(tokens[token.value]);
            tokens[token.value]=null;
        }
        Thread.sleep(100);
        assertNull(q.poll());
        for (int k=0; k<nt; k++) {
            assertNull(tokens[k]);
        }
        pong.close();
        for (int k=0; k<nw; k++) {
            final Token token = q.take();
            assertEquals(-1, token.value);
        }
        Thread.sleep(100);
        assertNull(q.poll());
    }

    static class Token extends IntValue {
        boolean touched;
        public Token(int value) {
            super(value);
        }
    }

    static class Pong extends Dispatcher<Token> {
        LinkedBlockingQueue<Token> q;
        boolean print;
        
        Pong(int nw, LinkedBlockingQueue<Token> q, boolean print){ 
            this.q=q;
            this.print=print;
            for (int k=0; k<nw; k++) {
                new PongWorker(k);
            }
        }

        /**
         * The ponging actor
         * 
         */
        class PongWorker extends Actor<Token> {
            int id;

            public PongWorker(int id) {
                this.id=id;
                listen(this);
            }
/*
            @Override
            protected Input<Token> createInput() {
                return new ScalarInput<Token>();
            }
*/
            @Override
            protected void act(Token message) throws Exception {
                if (print) {
                    System.out.println("  act:"+id+" m:"+message.value+" id="+message);
                    System.out.flush();
                }
                message.touched=true;
                q.add(message);
                listen(this);
            }

            @Override
            protected void complete() throws Exception {
                q.add(new Token(-1));
            }
        }
    }

    public static void main(String args[]) throws InterruptedException {
        DispatcherTest nt = new DispatcherTest();
        nt.test02();
    }

}
