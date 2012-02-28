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

import com.github.rfqu.df4j.core.Function;

/**
 * Binary operation: classic dataflow object.
 * Waits for both operands to arrive,
 * computes the operation, and sends result to the DataSource object,
 * which routes the result to the interested parties.
 *
 * @param <T> the type of operands and the result
 */
public abstract class BinaryOp<T> extends Function<T> {
    public ScalarInput<T> p1 = new ScalarInput<T>();
    public ScalarInput<T> p2 = new ScalarInput<T>();
    {start();}

    T operand1, operand2;
    
    @Override
    protected void removeTokens() {
        operand1=p1.remove();
        operand2=p2.remove();
    }

    @Override
    protected void fire() {
        // TODO Auto-generated method stub
        super.fire();
    }

    @Override
    protected void act() {
        setRes(eval(operand1, operand2));
    }

    abstract protected T eval(T opnd, T opnd2);

}