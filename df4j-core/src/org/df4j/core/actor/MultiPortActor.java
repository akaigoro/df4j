/*
 * Copyright 2011 by Alexei Kaigorodov
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.df4j.core.actor;

import java.util.concurrent.Executor;

import org.df4j.core.Port;
import org.df4j.core.ext.SerialExecutor;

/**
 * An Actor with several input Ports, subclassed from MultiPortActor.PortHandler.
 * Each port has specific message handler PortHandler.act(M m).
 * Messages for all ports are stored in the single message queue.
 */
public class MultiPortActor {
    protected final SerialExecutor execActor;

    public MultiPortActor() {
        execActor=new SerialExecutor();
    }

    public MultiPortActor(Executor executor) {
        execActor=new SerialExecutor(executor);
    }

	public void close() {
        execActor.execute(new Runnable(){
            @Override
            public void run() {
                MultiPortActor.this.complete();
            }
        });
	}

	//======= backend
	
    protected void complete() {
    }

    public abstract class PortHandler<M> implements Port<M> {

        @Override
        public final void post(final M m) {
            execActor.execute(new Runnable(){
                @Override
                public void run() {
                    PortHandler.this.act(m);            
                }
            
            });
        }
        
        protected abstract void act(M m);
    }
}
