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

import com.github.rfqu.df4j.core.BaseActor.BooleanPlace;
import com.github.rfqu.df4j.core.BaseActor.StreamInput;

/**
 * Actor with 2 ports.
 * @param <M> the type of accepted messages
 */
public abstract class TwoPortActor<M extends Link> extends BaseActor {
    public final StreamInput<M> left=new StreamInput<M>();
    public final StreamInput<M> right=new StreamInput<M>();
    { start(); }
	BooleanPlace notFired=new BooleanPlace(true); // 'true' state allows firing
	protected boolean completed;
	
	public void close() {
		left.close();
		right.close();
	}

	@Override
	protected void fire() {
		notFired.remove(); // to prevent multiple concurrent firings
		super.fire();
	}


	/** loops through pairs of accumulated messages
     */
	@Override
	public void run() {
        for (;;) {
            M messageL, messageR;
            synchronized (this) {
                if (left.isEmpty() || right.isEmpty()) {
                	notFired.send(); // allow firing
                    return;
                }
                messageL = left.remove();
                messageR = right.remove();
            }
            try {
				act(messageL, messageR);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        }
    }
    
    /**
     * processes one incoming message
     * @param message the message to process
     * @param messageR 
     * @throws Exception
     */
    protected abstract void act(M messageL, M messageR) throws Exception;

    /**
     * processes one incoming message
     * @throws Exception
     */
    protected abstract void complete() throws Exception;

}
