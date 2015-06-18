/*
 * Copyright 2012 by Alexei Kaigorodov
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.df4j.core.ext;

import org.df4j.core.Port;
import org.df4j.core.SharedPlace;

/**
 * In multithreaded programming, often several identical worker threads are fed with
 * a single input queue. If we want to replace threads with actors, this cannot be done
 * directly, as actors may not be blocked (which happens when the queue is empty). 
 * This sample code shows how to build a demultiplexer to feed several actors with single queue.
 * Actors work in parallel. 
 * The actor wanting to be fed sends itself to the actors port with {@link #listen(Actor<M>)}.
 */
public class Dispatcher<R> extends SharedPlace<R> {
    private final StreamInput<R> resources=new StreamInput<R>();
    
    /** Accepts request from the actor for the next message.
     * The next message will be sent to the actor as soon as it is available.
     * The request is served once, so after the message is processed by the actor,
     * the actor has to issue the request again. This way the actor can request messages
     * from different sources. 
     * The close signal is passed to all actors.
     * @param actor
     */
    public void post(Port<R> actor) {
        if (isClosed()) {
       //     actor.close(); TODO
        } else {
            super.post(actor);
        }
    }

    @Override
    protected void act(Port<R> request) throws Exception {
        request.post(resources.get());
    }
/* TODO
    @Override
    protected void complete() throws Exception {
        resources.close();
        for (StreamPort<R> actor: actors) {
            actor.close();
        }
    }
*/
    @Override
    public void ret(R token) {
        resources.post(token);
    }

}