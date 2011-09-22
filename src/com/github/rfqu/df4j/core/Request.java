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

import com.github.rfqu.df4j.core.Link;

/**
 * A message that carries callback port.
 * @param <R>
 */
public class Request<R> extends Link {
    public Port<R> callback;

    public Request() {
    }

    public Request(Port<R> callback) {
        this.callback = callback;
    }

    public void reply(R result) {
        if (callback == null)
            return;
        callback.send(result);
    }
}
