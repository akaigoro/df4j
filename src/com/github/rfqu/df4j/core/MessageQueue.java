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

import java.util.concurrent.ConcurrentLinkedQueue;

/** Simple one-way message queue
 * @param <M> the type of the enqueued messages
 */
public class MessageQueue<M extends Link> {
    protected M first;
    protected M last;

    /**
     * enqueues a message
     * @param message the message to enqueue
     * @throws NullPointerException when message is null
     * @throws IllegalArgumentException when message is already enqueued
     */
    public void enqueue(M message) {
        if (message==null) {
            throw new NullPointerException("message may not be null");
        }
        if (message.next!=null) {
            throw new IllegalArgumentException("message is already enqueued in another queue");
        }
        if (last==null) {
            first=last=message;
        } else {
            if (last==null) {
                throw new RuntimeException("last=null but first!=null");
            }
            last.next=message;
            last=message;
        }
    }
    
    /**
     * @return true if this queue is empty, false otherwise
     */
    public boolean isEmpty() {
        return first==null;
    }
    
    /**
     * @return the next message, or null if the queue is empty
     */
    @SuppressWarnings("unchecked")
	public M poll() {
        M res=first;
        if (res==null) {
            return null;
        }
        if (res==last) {
            first=last=null;
        } else {
            first=(M) res.next;
        }
        res.next=null;
        return res;
    }

}
