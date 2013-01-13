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

import java.util.concurrent.Executor;

import com.github.rfqu.df4j.core.Actor;
import com.github.rfqu.df4j.core.DoublyLinkedQueue;
import com.github.rfqu.df4j.core.Link;
import com.github.rfqu.df4j.core.Port;

/**
 * An Actor with several input Ports, subclassed from MultiPortActor.PortHandler.
 * Each port has specific message handler PortHandler.act(M m).
 * Handlers are determined by the exact type of Message, subclassed from MultiPortActor.Message.
 * The message type M need not to extend Link.
 * Messages for all ports are stored in the single message queue.
 */
public class MultiPortActor {
    protected final Actor<Message<?>> execActor;

    public MultiPortActor() {
        execActor=new ExecActor();
    }

    public MultiPortActor(Executor executor) {
        execActor=new ExecActor(executor);
    }

	public void close() {
		execActor.close();
	}

	//======= backend
	
    protected void complete() throws Exception {
    }

    private static final class Message<M> extends Link {
        private PortHandler<M> handler;
        private M m;
        
        Message(PortHandler<M> handler, M m) {
            this.handler=handler;
            this.m=m;
        }

        void act() {
            handler.act(m);            
        }
    }
    
    private final class ExecActor extends Actor<Message<?>> {
        public ExecActor() {
        }

        public ExecActor(Executor executor) {
            super(executor);
        }

        @Override
        protected Input<Message<?>> createInput() {
            return new StreamInput<Message<?>>(new DoublyLinkedQueue<Message<?>>());
        }

        @Override
        protected final void act(Message<?> message) throws Exception {
            message.act();
        }
        
        @Override
        protected void complete() throws Exception {
        	MultiPortActor.this.complete();
        }
    }
    
    public abstract class PortHandler<M> implements Port<M> {

        @Override
        public final void post(M m) {
            execActor.post(new Message<M>(this, m));
        }
        
        protected abstract void act(M m);
    }
}
