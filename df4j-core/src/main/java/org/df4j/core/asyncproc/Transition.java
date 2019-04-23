/*
 * Copyright 2011 by Alexei Kaigorodov
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.df4j.core.asyncproc;

import java.util.ArrayList;

/**
 * This class is named following Petri Nets terminology.
 *
 * This class contains base class {@link ScalarLock} for connectors (places),
 * and a mechanism to count the number of blocked pins.
 * As soon as all connectors are unblocked, the method {@link #fire()} is called.
 */
public abstract class Transition {
    private static final Object[] emptyArgs = new Object[0];

    /**
     * total number of created pins
     */
    private int blockedPinCount = 0;

    /**
     * number of parameter pins
     */
    private int paramCount = 0;

    /**
     * first paramCount items are parameters, and the rest - pure pins
     */
    protected ArrayList<ScalarLock> pins = new ArrayList<>();

    protected int getParamCount() {
        return paramCount;
    }

    protected synchronized Object[] collectArgs() {
        if (paramCount == 0) {
            return emptyArgs;
        } else {
            Object[] args = new Object[paramCount];
            for (int k = 0; k < paramCount; k++) {
                args[k] = pins.get(k).current();
            }
            return args;
        }
    }

    /**
     * usually submits this object as a task to a thread pool
     */
    protected abstract void fire();

    public synchronized void register(ScalarLock pin) {
        if (pin.isParameter()) {
            pins.add(paramCount, pin);
            paramCount++;
        } else {
            pins.add(pin);
        }
        // all pins are created blocked
        blockedPinCount++;
    }

    public synchronized void incBlocked() {
        blockedPinCount++;
    }

    public void decBlocked() {
        synchronized (this) {
            if (blockedPinCount == 0) {
                throw new RuntimeException();
            }
            blockedPinCount--;
            if (blockedPinCount > 0) {
                return;
            }
            // debug todo remove
            for (int k = 0; k < pins.size(); k++) {
                ScalarLock lock = pins.get(k);
                if (lock.isBlocked()) {
                    throw new RuntimeException();
                }
            }
        }
        fire();
    }
}
//V1.00(BWN.3)D0 21-Июн-2011 23:04