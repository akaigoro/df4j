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

import java.util.NoSuchElementException;

/** Simple doubly linked message queue
 * @param <M> the type of the enqueued messages
 */
public class LinkedQueue<M extends Link> extends Link {
    /**
     * @return true if this queue is empty, false otherwise
     */
    public boolean isEmpty() {
        return !isLinked();
    }

    /**
     * enqueues a message
     * @param newLink the message to enqueue
     * @throws IllegalArgumentException when message is null or is already enqueued
     */
    public void add(M newLink) {
        if (newLink==null) {
            throw new IllegalArgumentException("link is null");
        }
        if (newLink.isLinked()) {
            throw new IllegalArgumentException("link is linked already");
        }
        link(newLink);
    }

    /**
     * @return the next message, or null if the queue is empty
     */
    @SuppressWarnings("unchecked")
    public M poll() {
        if (isEmpty() ) {
            return null;
        }
        Link res = previous;
        res.unlink();
        return (M) res;
      }

    /**
     * @return the next message
     * @throws NoSuchElementException if the queue is empty
     */
    @SuppressWarnings("unchecked")
    public M remove() {
        if (isEmpty() ) {
            throw new NoSuchElementException();
        }
        Link res = previous;
        res.unlink();
        return (M) res;
      }

}
