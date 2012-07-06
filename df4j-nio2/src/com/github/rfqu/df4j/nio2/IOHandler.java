/*
 * Copyright 2012 by Alexei Kaigorodov
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package com.github.rfqu.df4j.nio2;

import java.util.concurrent.Executor;

import com.github.rfqu.df4j.core.Port;
import com.github.rfqu.df4j.core.Task;

/**
 * Handles the result of an IO operation.
 * Can be seen as a simplified actor, with space for only 1 incoming message.
 * @param <R> the type of accepted messages.
 */
public abstract class IOHandler<R extends IORequest<R, C>, C>
  extends Task implements Port<R>
{
    protected boolean fired=false; // true when this actor runs
    protected R message=null;

    public IOHandler(Executor executor) {
        super(executor);
    }

    public IOHandler() {
    }

    @Override
    public void send(R message) {
        synchronized (this) {
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
    protected void act(R request) throws Exception {
    	Throwable exc=request.getExc();
		if (exc!=null) {
			failed(exc, request);
    	} else {
    		completed(request.result, request);
    	}
    }

    /** handles failures
     * 
     * @param message
     * @param e
     */
    protected void failure(R message, Exception e) {
        e.printStackTrace();
    }
    
    protected abstract void completed(Integer result, R request) throws Exception;

    protected void failed(Throwable exc, R request) throws Exception {
    	exc.printStackTrace();
    }

}
