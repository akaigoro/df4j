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
 * A dataflow node with one stream input port.
 * @param <M> the type of accepted messages.
 */
public abstract class Actor<M extends Link> extends BaseActor implements StreamPort<M> {
	protected final StreamInput<M> input=new StreamInput<M>();
	/** true when the closing signal has been processed */
	protected boolean completed;
    protected long actCounter=0; // DEBUG
    protected long failureCounter=0; // DEBUG
	
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

    /** loops while all pins are ready
     */
    @Override
    public void run() {
        for (;;) {
            synchronized (this) {
                if (!isReady()) {
                    fired=false; // allow firing
                    return;
                }
                consumeTokens();
            }
            act();
        }
    }
    
    /** 
     * Removes tokens from pins.
     * Removed tokens are expected to be used used in the act() method.
     * Should remove at least 1 token to avoid infinite loop.
     * Should return quickly, as is called from synchronized block.
     */
    protected void consumeTokens() {
	    input.consume();
    }

    /** 
     * process the retrieved tokens.
     */
    protected void act() {
        M message=input.value;
        try {
            if (message == null) {
                complete();
            } else {
                actCounter++;
                act(message);
            }
        } catch (Exception e) {
            failureCounter++;
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

    /**
     * @return true when the method complete() has finished
     */
	public boolean isCompleted() {
		return completed;
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
