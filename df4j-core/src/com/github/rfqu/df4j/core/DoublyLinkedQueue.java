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

import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Queue;

/** Simple doubly linked message queue
 * @param <M> the type of the enqueued messages
 */
public class DoublyLinkedQueue<M extends Link> extends Link implements Queue<M> {
    /**
     * @return true if this queue is empty, false otherwise
     */
    public boolean isEmpty() {
        return !isLinked();
    }

    /**
     * enqueues a message
     * @param newLink the message to enqueue
     * @return 
     * @throws IllegalArgumentException when message is null or is already enqueued
     */
   @Override
   public boolean add(M newLinkp) {
        Link newLink=(Link)newLinkp;
        if (newLink==null) {
            throw new IllegalArgumentException("link is null");
        }
        if (newLink.isLinked()) {
            throw new IllegalArgumentException("link is linked already");
        }
        link(newLink);
        return true;
    }

    /**
     * @return the next message, or null if the queue is empty
     */
    @Override
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
    @Override
    @SuppressWarnings("unchecked")
    public M remove() {
        if (isEmpty() ) {
            throw new NoSuchElementException();
        }
        Link res = previous;
        res.unlink();
        return (M) res;
      }

    @Override
    public int size() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public boolean contains(Object o) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public Iterator<M> iterator() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Object[] toArray() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <T> T[] toArray(T[] a) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean remove(Object o) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean addAll(Collection<? extends M> c) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void clear() {
        // TODO Auto-generated method stub
        
    }

    @Override
    public boolean offer(M e) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public M element() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public M peek() {
        // TODO Auto-generated method stub
        return null;
    }

}
