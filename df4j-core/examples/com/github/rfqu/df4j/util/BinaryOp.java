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

import com.github.rfqu.df4j.core.DataSourceSingle;
import com.github.rfqu.df4j.core.Port;
import com.github.rfqu.df4j.core.Promise;
import com.github.rfqu.df4j.core.Task;

/**
 * Binary operation: classic dataflow object.
 * Waits for both operands to arrive,
 * computes the operation, and sends result to the DataSource object,
 * which routes the result to the interested parties.
 *
 * @param <R> the type of operands and the result
 */
public abstract class BinaryOp<T, R> extends Task implements Promise<R> {
    public Inp p1 = new Inp();
    public Inp p2 = new Inp();
    public DataSourceSingle<R> res = new DataSourceSingle<R>();

    public class Inp implements Port<T> {
        protected boolean opndready = false;
        T operand;

        @Override
        public BinaryOp<T, R> send(T v) {
            synchronized (this) {
                operand = v;
                opndready = true;
                if (!(p1.opndready && p2.opndready)) {
                    return BinaryOp.this;
                }
            }
            fire();
            return BinaryOp.this;
        }
    }

    @Override
    public <S extends Port<R>> S request(S sink) {
        res.request(sink);
        return sink;
    }

    @Override
    public void run() {
        try {
            R result = operation(p1.operand, p2.operand);
            res.send(result);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    abstract protected R operation(T opnd, T opnd2);
}