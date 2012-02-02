/*
 * Copyright 2011 by Alexei Kaigorodov
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package com.github.rfqu.df4j.util;

import com.github.rfqu.df4j.core.OutPort;

/**
 * collects values from  maxCount tokens and send the sum to the specified port
 * @author rfqu
 *
 */
public class IntAggregator  implements OutPort<Integer> {
    OutPort<Integer> port;
    int maxCount=Integer.MAX_VALUE;
    int eventCount = 0;
    int value = 0;

    public IntAggregator(OutPort<Integer> caller) {
        this.port = caller;
    }

    @Override
    public void send(Integer delta) {
        synchronized (this) {
            eventCount++;
            value += delta;
            if (eventCount < maxCount) {
                return;
            }
        }
        port.send(new Integer(value));
    }

	public void setCount(int resultcount) {
        synchronized (this) {
        	maxCount=resultcount;
            if (eventCount < maxCount) {
                return;
            }
        }
        port.send(new Integer(value));
	}
}
