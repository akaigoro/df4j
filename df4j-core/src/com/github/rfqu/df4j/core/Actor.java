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
 * Processes messages in asynchronous way.
 * @param <M> the type of accepted messages
 */
public abstract class Actor<M extends Link> extends Task implements StreamPort<M> {
	private final boolean eager;
    private MessageQueue<M> input=new MessageQueue<M>();
    protected boolean closed=false;
    protected boolean completed=false;
    protected boolean fired;
    protected boolean ready=true;
    
    /**
     * @param eager if false, the actor would process messages immediate a
     * at the invocation of 'send', if no other messages are pending.
     */
    public Actor(boolean eager) {
        this.eager=eager;
    }

    public Actor() {
        this(false);
    }

	/**
     * @param ready allows/prohibit actor to process messages
     */
    public void setReady(boolean ready) {
        synchronized(this) {
            this.ready=ready;
            if (fired || input.isEmpty() || closed || !ready) {
                return;
            }
            fired=true;
        }
        fire();
    }

    public void send(M message) {
        if (message==null) {
            throw new NullPointerException("message may not be null");
        }
        synchronized (this) {
            if (closed) {
                throw new IllegalStateException("the queue is closed");
            }
            if (message.isLinked()) {
                throw new IllegalArgumentException("message is already enqueued in another queue");
            }
            if (fired || !ready || !eager) {
                input.add(message);
                if (fired || !ready) {
                    return;
                }
            }
            fired=true;
        }
        if (eager) {
            try {
				act(message);
			} catch (Exception e) {
				failure(message, e);
			} finally {
				synchronized (this) {
				    if (input.isEmpty()) {
				    	fired=false;
				        return;
				    }
				    fired=true;
				}
			}
        }
        fire();
    }

	public  void close(){
        synchronized(this) {
            closed=true;
            if (fired || !ready) {
                return;
            }
        }
        fire();
    }    

	public boolean isClosed() {
		return closed;
	}
        
	public boolean isCompleted() {
		return completed;
	}

    /** loops through the accumulated message queue
     */
	@Override
	public void run() {
        for (;;) {
            M message;
            synchronized (this) {
                message = input.poll();
                if ((message == null) && !closed) {
                	fired=false;
                    return;
                }
            }
            doAct(message);
        }
    }

    private void doAct(M message) {
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
     * processes one incoming message
     * @throws Exception
     */
    protected abstract void complete() throws Exception;

}
