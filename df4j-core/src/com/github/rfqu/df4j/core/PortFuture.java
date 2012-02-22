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

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Connects Actor's world and the outer space.
 * Actors are allowed to send messages to it, but not to get from
 *
 * @param <T> the type of accepted messages
 */
public class PortFuture<T> extends Link implements Port<T>, Future<T> {
    protected volatile T message;
    
    /**
     * (non-Javadoc)
     * @return 
     * @see com.github.rfqu.df4j.core.Port#send(java.lang.Object)
     */
    @Override
    public synchronized void send(T message) {
        this.message=message;
        notifyAll();
    }

    /**
     * waits until a message arrive
     * @return received message
     * @throws InterruptedException if the current thread was interrupted while waiting
     */
    @Override
    public synchronized T get() throws InterruptedException {
        while (message==null) {
            wait();
        }
        return (T) message;
    }

    /**
     * checks if the message has arrived.
     * @return received message, or null if no message arrived
     */
    public synchronized T poll() {
        return (T) message;
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isCancelled() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isDone() {
        return message!=null;
    }

    @Override
    public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        if (message!=null) {
            return message;
        }
        long duration = unit.toMillis(timeout);
        long startTime=System.currentTimeMillis();
        long endTime=startTime+duration;
        for (;;) {
            wait(duration);            
            if (message!=null) {
                return message;
            }
            long currentTime=System.currentTimeMillis();
            duration=(endTime-currentTime);
            if (duration<=0) {
                throw new TimeoutException();
            }
        }
    }

}
