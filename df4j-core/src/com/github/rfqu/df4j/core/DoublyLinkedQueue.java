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

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.AbstractQueue;

/** Simple doubly linked message queue
 * @param <M> the type of the enqueued messages
 */
public class DoublyLinkedQueue<M extends Link> extends AbstractQueue<M> {
    private final Link head=new Link();
    
    /**
     * @return true if this queue is empty, false otherwise
     */
    public boolean isEmpty() {
        return !head.isLinked();
    }

    /**
     * enqueues a message
     * @param newLink the message to enqueue
     * @return 
     * @throws IllegalArgumentException when message is null or is already enqueued
     */
   @Override
   public boolean add(M newLink) {
        if (newLink==null) {
            throw new IllegalArgumentException("link is null");
        }
        if (newLink.isLinked()) {
            throw new IllegalArgumentException("link is linked already");
        }
        head.link(newLink);
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
        Link res = head.previous;
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
        Link res = head.previous;
        res.unlink();
        return (M) res;
      }

    @Override
    public boolean offer(M newLink) {
        if (newLink==null) {
            throw new IllegalArgumentException("link is null");
        }
        if (newLink.isLinked()) {
            throw new IllegalArgumentException("link is linked already");
        }
        head.link(newLink);
        return true;
    }

    @SuppressWarnings("unchecked")
    @Override
    public M peek() {
        if (isEmpty() ) {
            throw new NoSuchElementException();
        }
        Link res = head.previous;
        return (M) res;
    }

    @Override
    public Iterator<M> iterator() {
        return new Iterator<M>(){
            Link cursor=head;
            @Override
            public boolean hasNext() {
                return cursor.previous!=head;
            }

            @SuppressWarnings("unchecked")
            @Override
            public M next() {
                cursor=cursor.previous;
                return (M) cursor;
            }

            @Override
            public void remove() {
                if (cursor==head) {
                    throw new IllegalStateException();
                }
                Link elem=cursor;
                cursor=cursor.previous;
                elem.unlink();
            }
            
        };
    }

    @Override
    public int size() {
        int res=0;
        for (Link link=head.next; link!=head; link=link.next) {
            res++;
        }
        return res;
    }
}
