/*
 * Copyright 2011 by Alexei Kaigorodov
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package com.github.rfqu.df4j.ext;

import com.github.rfqu.df4j.core.Callback;

/**
 * Unary operation
 *
 * @param <T> type of the operand and the result
 */
public abstract class UnaryOp<T> extends Function<T> implements Callback<T> {
    protected CallbackInput<T> input=new CallbackInput<T>();

    @Override
    public void send(T value) {
        input.send(value);
    }

    @Override
    public void sendFailure(Throwable exc) {
        input.sendFailure(exc);
    }

    @Override
    protected T eval() {
        return eval(input.value);
    }

    abstract protected T eval(T operand);

}