/*
 * Copyright 2011 by Alexei Kaigorodov
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package com.github.rfqu.df4j.core;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.PrintStream;
import java.util.concurrent.LinkedBlockingQueue;

import org.junit.Test;

import com.github.rfqu.df4j.core.Actor;
import com.github.rfqu.df4j.core.DoublyLinkedQueue;
import com.github.rfqu.df4j.core.MessageQueue;
import com.github.rfqu.df4j.util.IntValue;

/**
 * In multithreaded programming, often several identical working threads are fed with
 * a single input queue. If we want to replace threads with actor, this cannot be done
 * directly, as actors may not be blocked (when the queue is empty). 
 * This sample code shows how to build a demultiplexer to feed several actors with single queue.
 * Actors work in parallel. 
 * The actor wanting to be fed sends itself to the actors port with Demux.listen(this).
 */
public class MessageQueueTest {
    int nThreads;
    PrintStream out = System.out;

    @Test
    public void test1() throws InterruptedException {
        Pong1 pong=new Pong1();
        for (int k=0; k<10; k++) {
            pong.send(new IntValue(k));
        }
    }

    /** checks that all sent tokens are processed
     */
    @Test
    public void test2() throws InterruptedException {
        for (int k=1; k<6; k++) {
            testN(10*k*k, k);
        }
    }

    /**
     * various numbers of tokens and workers 
     * @param nt number of tokens
     * @param nw number of workers
     * @throws InterruptedException
     */
    public void testN(int nt, int nw) throws InterruptedException {
        LinkedBlockingQueue<Token> q=new LinkedBlockingQueue<Token>();
        Pong2 pong=new Pong2(nw,q);
        Token[] tokens=new Token[nt];
        for (int k=0; k<nt; k++) {
            final Token token = new Token(k);
            pong.send(token);
            tokens[k]=token;
        }
        for (int k=0; k<nt; k++) {
            final Token token = q.take();
            assertTrue(token.touched);
            assertNotNull(tokens[token.value]);
            tokens[token.value]=null;
        }
        assertNull(q.poll());
        for (int k=0; k<nt; k++) {
            assertNull(tokens[k]);
        }
    }

    static class Pong1 extends MessageQueue<IntValue> {
        { 
            for (int k=0; k<3; k++) {
                new PongWorker(k);
            }
        }
        
        @Override
        protected Input<IntValue> createInput() {
            return new StreamInput<IntValue>(new DoublyLinkedQueue<IntValue>());
        }
        
        @Override
        protected void act() {
            PongWorker actor = (PongWorker)actors.value;
            IntValue message = input.value;
            if (message==null) {
                // input closed
                actor.close();
            } else {
                System.out.println("send:"+actor.id+" m:"+message.value);
                System.out.flush();
                actor.send(message);
            }
        }
        
        /**
         * The ponging actor
         * 
         */
        class PongWorker extends Actor<IntValue> {
            int id;

            public PongWorker(int id) {
                this.id=id;
                addListener(this);
            }

            @Override
            protected void act(IntValue message) throws Exception {
                System.out.println("  act:"+id+" m:"+message.value);
                System.out.flush();
                addListener(this);
            }
        }
    }
    
    static class Token extends IntValue {
        boolean touched;
        public Token(int value) {
            super(value);
        }
        
    }

    static class Pong2 extends MessageQueue<Token> {
        LinkedBlockingQueue<Token> q;

        Pong2(int nw, LinkedBlockingQueue<Token> q){ 
            this.q=q;
            for (int k=0; k<nw; k++) {
                new PongWorker(k);
            }
        }

        @Override
        protected Input<Token> createInput() {
            return new StreamInput<Token>(new DoublyLinkedQueue<Token>());
        }
        /**
         * The ponging actor
         * 
         */
        class PongWorker extends Actor<Token> {
            int id;

            public PongWorker(int id) {
                this.id=id;
                addListener(this);
            }

            @Override
            protected Input<Token> createInput() {
                return new ScalarInput<Token>();
            }

            @Override
            protected void act(Token message) throws Exception {
                message.touched=true;
                q.add(message);
                addListener(this);
            }
        }
    }

    public static void main(String args[]) throws InterruptedException {
        MessageQueueTest nt = new MessageQueueTest();
        nt.test1();
    }

}
