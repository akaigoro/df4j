/*
 * Copyright 2011 by Alexei Kaigorodov
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package com.github.rfqu.dffw.core;

/**
 * Processes messages in asynchronous way using current executor.
 * Actors themselves are messages and can be send to other Actors and Ports
 * @param <M> the type of accepted messages
 */
public abstract class Actor<M extends Link> extends Task implements Port<M> {
    MessageQueue<M> input=new MessageQueue<M>();
    protected boolean ready=true;

    public Actor() {
    }
    
    /**
     * @param ready if false, the actor would not process messages until
     *  invocation of setReady(true)
     */
    public Actor(boolean ready) {
        this.ready=ready;
    }
    
    /**
     * (non-Javadoc)
     * @return 
     * @see com.github.rfqu.dffw.core.Port#send(java.lang.Object)
     */
    @Override
    public Actor<M> send(M message) {
        synchronized(this) {
            input.enqueue(message);
            if (running) {
                return this;
            } else if (ready) {
                running=true;
            }
        }
        fire();
        return this;
    }

    /**
     * @param ready allows/prohibit actor to process messages
     */
    public void setReady(boolean ready) {
        synchronized(this) {
            this.ready=ready;
            if (running) {
                return;
            } else if (ready && !input.isEmpty()) {
                running=true;
            }
        }
        fire();
    }

    /**
     * processes one incoming message
     * @param message the message to process
     * @throws Exception
     */
    protected abstract void act(M message) throws Exception;

    /** loops through the accumulated message queue
     */
    @Override
    public void run() {
        for (;;) {
            M message;
            synchronized (this) {
                message = input.poll();
                if (message == null) {
                    running = false;
                    return;
                }
            }
            try {
                act(message);
            } catch (Exception e) {
                failure(message, e);
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
    
}
