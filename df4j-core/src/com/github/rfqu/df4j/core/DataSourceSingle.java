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

/**
 * A kind of dataflow variable: single input, single asynchronous outputs.
 * @param <T> the type of operands and the result
 */
public class DataSourceSingle<R> implements Promise<R>, Port<R> {
    protected volatile R result;
    protected Port<R> sink;

    @Override
    public synchronized DataSourceSingle<R> send(R result) {
        this.result = result;
        if (sink == null) {
            return this;
        }
        try {
            sink.send(result);
        } finally {
            sink=null;
        }
        return this;
    }

    @Override
    public synchronized <S extends Port<R>> S request(S sink) {
        if (sink == null) {
            throw new NullPointerException();
        }
        if (result != null) {
            sink.send(result);
            return sink;
        }
        if (this.sink != null) {
            throw new IllegalStateException("Cannot handle multiple ports");
        }
        this.sink=sink;
        return sink;
    }


}