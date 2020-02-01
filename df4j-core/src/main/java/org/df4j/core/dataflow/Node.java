/*
 * Copyright 2011 by Alexei Kaigorodov
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.df4j.core.dataflow;

import org.df4j.core.communicator.Completion;
import org.df4j.core.util.linked.Link;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public abstract class Node<T> extends Completion implements Link<T> {
    private Link<T> prev = this;
    private Link<T> next = this;
    protected Dataflow parent;

    @Override
    public Link<T> getNext() {
        return next;
    }

    @Override
    public void setNext(Link<T> next) {
        this.next = next;
    }

    @Override
    public Link<T> getPrev() {
        return prev;
    }

    @Override
    public void setPrev(Link<T> prev) {
        this.prev = prev;
    }
}