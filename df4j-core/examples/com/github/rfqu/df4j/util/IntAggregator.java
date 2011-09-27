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

import com.github.rfqu.df4j.core.Port;

/**
 * collects values from  maxCount tokens and send suns to the specified port
 * @author rfqu
 *
 */
public class IntAggregator  implements Port<Integer> {
    Port<Integer> port;
    int maxCount;
    int eventCount = 0;
    int value = 0;

    public IntAggregator(int maxTokenCount, Port<Integer> caller) {
        this.maxCount = maxTokenCount;
        this.port = caller;
    }

    @Override
    public Object send(Integer delta) {
        synchronized (this) {
            eventCount++;
            value += delta;
            if (eventCount != maxCount) {
                return this;
            }
        }
        port.send(new Integer(value));
        return this;
    }
}
