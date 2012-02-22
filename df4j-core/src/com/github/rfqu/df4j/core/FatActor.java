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

import java.util.concurrent.ExecutorService;

/**
 * Processes messages in asynchronous way using its own thread.
 * @param <M> the type of accepted messages
 */
public abstract class FatActor<M extends Link> extends Actor<M> {
	
    private ExecutorService myExecutor;

	public FatActor(ExecutorService executor) {
		if (executor==null) {
	    	myExecutor=ThreadFactoryTL.newSingleThreadExecutor();
		} else {
	    	myExecutor=ThreadFactoryTL.newSingleThreadExecutor(executor);
		}
    }

    public FatActor() {
    	this(Task.getCurrentExecutor());
    }

    @Override
	protected synchronized void fire() {
		notFired.remove(); // für ordnung
		myExecutor.execute(this);
	}
}
