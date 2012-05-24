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

import java.util.concurrent.Executor;

/**
 * An Actors with several input Ports
 */
public class MultiPortActor extends Actor<MultiPortActor.Message<?>> {

    public MultiPortActor() {
	}

	public MultiPortActor(Executor executor) {
		super(executor);
	}

	@Override
	protected void complete() throws Exception {
		super.close();
	}

    @Override
    protected final void act(Message<?> message) throws Exception {
        message.act();
    }

    
    public static class Message<M> extends Link {
        PortHandler<M> handler;
        private M m;
        
        public Message(PortHandler<M> handler, M m) {
            this.handler=handler;
            this.m=m;
        }

        private void act() {
            handler.act(m);            
        }
    }
    
    protected abstract class PortHandler<M> implements Port <M>{

        @Override
        public final void send(M m) {
            MultiPortActor.this.send(new Message<M>(this, m));
        }
        
        protected abstract void act(M m);
    }
}
