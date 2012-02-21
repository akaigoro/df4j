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

/**
 * Processes messages in asynchronous way using its own thread.
 * @param <M> the type of accepted messages
 */
public abstract class FatActor<M extends Link> extends Actor<M> {
    protected Thread thread=new Thread(this);
    {
    	thread.setDaemon(true);
    }
    
    public void start() {
        thread.start();
	}

    @Override
	protected synchronized void fire() {
    	notify();
	}

    @Override
    public void run() {
        Task.setCurrentExecutor(executor);
        for (;;) {
            super.run();
            synchronized (this) {
                if (completed) {
                	return;
                }
                for (;;) {
                    if (fired) {
                        break;
                    }
                    try {
                        wait();
                    } catch (InterruptedException e) {
                        return;
                    }
                }
            }
        }
    }

}
