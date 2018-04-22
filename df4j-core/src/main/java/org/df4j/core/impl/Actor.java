/*
 * Copyright 2011 by Alexei Kaigorodov
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.df4j.core.impl;

import org.df4j.core.impl.messagestream.PickPoint;

/**
 * Actor is reusable AsynchronousCall: after execution, it executes again as soon as new array of arguments is ready
 *
 * An actor is like a Petri Net trasnsition with own places for tokens.
 * Shared places cannot be represented directly. To some extent, the role
 * of shared places is played by {@link PickPoint}
 */
public abstract class Actor extends AsynchronousCall {
    private Pin controlPin = new Pin(false);

    /**
     * invoked when all transition transition are ready,
     * and method run() is to be invoked.
     * Safe way is to submit this instance as a Runnable to an Executor.
     * Fast way is to invoke it directly, but make sure the chain of
     * direct invocations is limited to avoid stack overflow.
     */
    protected void fire() {
        controlPin.turnOff();
        super.fire();
    }

    @Override
    public void run() {
        try {
            act();
            consumeTokens();
            controlPin.turnOn();
        } catch (Throwable e) {
            System.err.println("Error in actor " + getClass().getName());
            e.printStackTrace();
        }
    }
}