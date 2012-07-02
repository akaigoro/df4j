/*
 * Copyright 2012 by Alexei Kaigorodov
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
public abstract class AsyncHandler<M> extends Task implements Port<M> {
    protected boolean fired=false; // true when this actor runs
    protected M message=null;

    public AsyncHandler(Executor executor) {
        super(executor);
    }

    public AsyncHandler() {
    }

    @Override
    public void send(M message) {
        synchronized (this) {
            if (this.message!=null) {
                throw new IllegalStateException("place is occupied already"); 
            }
            if (fired) {
                throw new IllegalStateException("fired already"); 
            }
            this.message=message;
            fired=true;
        }
        fire();
    }

    @Override
    public void run() {
        try {
            act(message);
        } catch (Exception e) {
            failure(message, e);
        } finally {
            synchronized (this) {
                fired=false;
            }
        }
    }

    /**
     * processes one incoming message
     * @param message the message to process
     * @throws Exception
     */
    protected abstract void act(M message) throws Exception;

    /** handles failures
     * 
     * @param message
     * @param e
     */
    protected void failure(M message, Exception e) {
        e.printStackTrace();
    }
    
}
