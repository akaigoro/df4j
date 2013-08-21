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

import java.util.concurrent.Executor;

/**
 * General dataflow node with several inputs and outputs.
 * Firing occur when all inputs are filled.
 * It is user's responsibility to consume tokens and avoid cycling.
 * Typical use case is:
 *  - create 1 or more pins for inputs and/or outputs
 *  - redefine abstract method act()
 */
public abstract class DataflowVertex extends DataflowVariable {
    
    public DataflowVertex(Executor executor) {
        super(new VertexTask(executor));
    }

    public DataflowVertex() {
        super(new VertexTask(null));
    }

    @Override
    protected abstract void act();
    
}