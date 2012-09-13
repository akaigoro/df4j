/*
 * Copyright 2011 by Alexei Kaigorodov
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package com.github.rfqu.df4j.actordemux;

/** Associative array of decoupled actors. Yes, this is tagged dataflow.
 * 
 * @param Tag type of key
 * @param M type of messages for actors
 * @param H type of handler
 */
public abstract class LiberalDemux<Tag, H>
	extends AbstractDemux<Tag, Action<Tag, H>, H>
{
    protected AbstractDelegator<Tag, Action<Tag, H>, H> createDelegator(Tag tag) {
        return new AbstractDelegator<Tag, Action<Tag, H>, H>(tag) {

            @Override
            protected void act(Action<Tag, H> message) throws Exception {
                message.act(tag, handler.token);     
            }
            
        };
    }
}
