/*
 * Copyright 2011 by Alexei Kaigorodov
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package com.github.rfq.dffw.util;

import com.github.rfq.dffw.core.Port;

/**
 * Connects Actor's world and outer space.
 * Actors are allowed to send messages to it, but not to get from
 *
 * @param <T> the type of accepted messages
 */
public class Promise<T> implements Port<T> {
    private T message;
    
    /**
     * (non-Javadoc)
     * @return 
     * @see com.github.rfq.dffw.core.Port#send(java.lang.Object)
     */
    @Override
    public synchronized Promise<T> send(T message) {
        this.message=message;
        notifyAll();
        return this;
    }

    /**
     * waits until a message arrive
     * @return received message
     * @throws InterruptedException if the current thread was interrupted while waiting
     */
    @SuppressWarnings("unchecked")
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
    @SuppressWarnings("unchecked")
    public synchronized T poll() {
        return (T) message;
    }

}
