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
import com.github.rfqu.df4j.core.*;

import java.util.concurrent.ConcurrentHashMap;


/** Associative array of decoupled actors. Yes, this is tagged dataflow.
 * 
 * @author rfq
 *
 * @param <Tag>
 * @param <H>
 */
public abstract class AbstractDemux<Tag, M extends Link, H>
	implements TaggedPort <Tag, M>
{
    protected ConcurrentHashMap<Tag, AbstractDelegator<M, H>> cache
    	= new ConcurrentHashMap<Tag, AbstractDelegator<M, H>>();

    @Override
    public void send(Tag tag, M message) {
        boolean dorequest=false;
        AbstractDelegator<M, H> gate=cache.get(tag);
        if (gate==null) {
            synchronized (this) {
                gate=cache.get(tag);
                if (gate==null) {
                    gate=createDelegator(tag);
                    cache.put(tag, gate);
                    dorequest=true;
                }
            }
        }
        if (dorequest) {
            requestHandler(tag, gate);
        }
        gate.send(message);
    }

    protected abstract AbstractDelegator<M,H> createDelegator(Tag tag);
    protected abstract void requestHandler(Tag tag, AbstractDelegator<M,H> gate);
}
