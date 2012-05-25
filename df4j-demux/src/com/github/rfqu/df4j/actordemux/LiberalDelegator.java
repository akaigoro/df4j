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

/**
 * Communication part of decoupled actor (delegator).
 * @param <H> Computational part of decoupled actor (delegate).
 * @author kaigorodov
 */
public class LiberalDelegator<H extends Delegate<Action<H>>> 
extends AbstractDelegator<Action<H>, H>{
    
    @Override
    protected void act(Action<H> message) throws Exception {
        message.act(handler.get());     
    }

    @Override
    protected void complete() throws Exception {
        // TODO Auto-generated method stub        
    }

}
