/*
 * Copyright 2012 by Alexei Kaigorodov
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package com.github.rfqu.df4j.ext;

import com.github.rfqu.df4j.core.AbstractActor;
import com.github.rfqu.df4j.core.Actor;
import com.github.rfqu.df4j.core.Link;
import com.github.rfqu.df4j.core.StreamPort;

/**
 * In multithreaded programming, often several identical worker threads are fed with
 * a single input queue. If we want to replace threads with actors, this cannot be done
 * directly, as actors may not be blocked (which happens when the queue is empty). 
 * This sample code shows how to build a demultiplexer to feed several actors with single queue.
 * Actors work in parallel. 
 * The actor wanting to be fed sends itself to the actors port with Demux.listen(this).
 */
public class Demux<M extends Link> extends AbstractActor implements StreamPort<M> {
    protected final StreamInput<M> input=new StreamInput<M>();
    protected final StreamInput<Actor<M>> actors=new StreamInput<Actor<M>>();
    
    protected Demux() {
        super(null); // the act method executes synchronously, without an executor.
    }

    /** Accepts request from the actor for the next message.
     * The next message will be sent to the actor as soon as it is available.
     * The request is served once, so after the message is processed by the actor,
     * the actor has to issue the request again. This way the actor can request messages
     * from different sources. 
     * @param actor
     */
    public void listen(Actor<M> actor) {
        actors.send(actor);
    }

    @Override
    public void send(M token) {
        input.send(token);
    }

    @Override
    public void close() {
        input.close();
    }
    
    @Override
    protected void act() {
        Actor<M> actor = actors.value;
        M value = input.value;
        if (value==null) {
            // input closed
            actor.close();
        } else {
            actor.send(value);
        }
    }

}