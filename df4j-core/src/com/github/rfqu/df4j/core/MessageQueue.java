/*
 * Copyright 2012 by Alexei Kaigorodov
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package com.github.rfqu.df4j.core;

import java.util.ArrayDeque;


/**
 * In multithreaded programming, often several identical worker threads are fed with
 * a single input queue. If we want to replace threads with actors, this cannot be done
 * directly, as actors may not be blocked (which happens when the queue is empty). 
 * This sample code shows how to build a demultiplexer to feed several actors with single queue.
 * Actors work in parallel. 
 * The actor wanting to be fed sends itself to the actors port with Demux.listen(this).
 */
public class MessageQueue<M> extends ActorVariable<M> implements EventSource<M, StreamPort<M>>{
    protected final StreamInput<StreamPort<M>> actors=createActorQueue();
    
    protected StreamInput<StreamPort<M>> createActorQueue() {
        return new StreamInput<StreamPort<M>>(new ArrayDeque<StreamPort<M>>());
    }

    /** Accepts request from the actor for the next message.
     * The next message will be sent to the actor as soon as it is available.
     * The request is served once, so after the message is processed by the actor,
     * the actor has to issue the request again. This way the actor can request messages
     * from different sources. 
     * @param actor
     */
    @Override
    public EventSource<M, StreamPort<M>> addListener(StreamPort<M> actor) {
        actors.send(actor);
        return this;
    }

    @Override
    protected void act(M message) throws Exception {
        StreamPort<M> actor = actors.value;
        if (message==null) {
            // input closed
            actor.close();
        } else {
            actor.send(message);
        }
    }

}