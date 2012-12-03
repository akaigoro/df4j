/*
 * Copyright 2011 by Alexei Kaigorodov
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package com.github.rfqu.df4j.ext;

import com.github.rfqu.df4j.core.ActorVariable;
import com.github.rfqu.df4j.core.DoublyLinkedQueue;
import com.github.rfqu.df4j.core.Link;

/**
 * An Actor with DoublyLinkedQueue as unbounded input queue, thus avoiding new instance creation
 * when a message is enqueued. Is able to accept messages of type M extends Link only. 
 * Used in tests for performance measurement.
 */
public abstract class ActorVariableDLQ<M extends Link> extends ActorVariable<M> {

	@Override
    protected Input<M> createInput() {
        return new StreamInput<M>(new DoublyLinkedQueue<M>());
    }
}
