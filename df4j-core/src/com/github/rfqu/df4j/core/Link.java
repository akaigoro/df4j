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

/**
 * The base class of messages and actors (actors are also messages).
 * Allows messages and actors to be enqueued without instantiating a wrapper object.
 * Also serves as a queue head.
 */
public class Link {
    protected Link next;
    protected Link previous;
    {
        next = previous = this;
    }

    public boolean isLinked() {
        return next != this;
    }

    /**
     * links <newLink> after this link
     */
    void link(Link newLink) {
        newLink.next = next;
        newLink.previous = this;
        next.previous = newLink;
        next = newLink;
    }

    void unlink() {
        next.previous = previous;
        previous.next = next;
        next = previous = this;
    }

}