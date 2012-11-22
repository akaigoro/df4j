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

import java.io.Closeable;
import java.util.ArrayDeque;
import java.util.concurrent.Executor;

/**
 * A dataflow node with one input stream port.
 * This is classic Actor type.
 * @param <M> the type of accepted messages.
 */
public abstract class Actor<M> extends DataflowNode
    implements StreamPort<M>, Callback<M>, Closeable
{
    /** place for input token(s) */
	protected final Input<M> input=createInput();
	
    public Actor(Executor executor) {
    	super(executor);
    }

    public Actor() {
    }
    
    /** Override this method if another type of input queue is desired. 
     * @return storage for input tokens
     */
    protected Input<M> createInput() {
        return new StreamInput<M>(new ArrayDeque<M>());
    }

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

    public boolean isClosed() {
        return input.isClosed();
    }

    //====================== backend
    
    /** 
     * process the retrieved tokens.
     */
    @Override
    protected void act() {
        M message=input.get();
        try {
            if (message==null) {
                complete();
            } else {
                act(message);
            }
        } catch (Exception e) {
            failure(message, e);
        }
    }

    /** handles failures
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
     * processes closing signal
     * @throws Exception
     */
    protected void complete() throws Exception {}

}
