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
 * A dataflow node with one stream port.
 * @param <M> the type of accepted messages.
 */
public abstract class Actor<M extends Link> extends BaseActor implements StreamPort<M> {
	protected final StreamInput<M> input=new StreamInput<M>();
    protected volatile boolean completed;
	
    public Actor(Executor executor) {
    	super(executor);
    }

    public Actor() {}

    @Override
	public void send(M m) {
		if (isClosed()) {
			throw new IllegalStateException();
		}
		input.send(m);
	}

	@Override
	public void close() {
		input.close();
	}

	@Override
    protected void retrieveTokens() {
	    input.retrieve();
    }

    @Override
    protected void act() {
        M message=input.token;
        try {
            if (message == null) {
                complete();
            } else {
                act(message);
            }
        } catch (Exception e) {
            failure(message, e);
        } finally {
            if (message == null) {
                completed=true;
            }
        }
    }

    public boolean isClosed() {
		return input.isClosed();
	}

	public boolean isCompleted() {
		return completed;
	}

	/** handles the failure
     * 
     * @param message
     * @param e
     */
    protected void failure(M message, Exception e) {
        e.printStackTrace();
    }
    
    /**
     * processes one incoming message
     * @param message the message to process
     * @throws Exception
     */
    protected abstract void act(M message) throws Exception;

    /**
     * processes closing message
     * @throws Exception
     */
    protected void complete() throws Exception {}

}
