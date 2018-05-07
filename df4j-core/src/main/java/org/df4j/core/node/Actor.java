/*
 * Copyright 2011 by Alexei Kaigorodov
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.df4j.core.node;

/**
 * Actor is a reusable Asynchronous Procedure Call: after execution, it executes again as soon as new array of arguments is ready.
 *
 * It consists of asynchronous connectors, implemented as inner classes,
 * user-defined asynchronous procedure, and a asyncTask mechanism to call that procedure
 * using supplied {@link java.util.concurrent.Executor} as soon as all connectors are unblocked.
 *
 * This class contains connectors for following protocols:
 *  - scalar messages
 *  - message stream (without back pressure)
 *  - permit stream
 */
public abstract class Actor extends AsyncTask {
    private volatile boolean started = false;
    private volatile boolean stopped = false;

    /**
     * blocked initially, until {@link #start} called.
     * blocked when this actor goes to executor, to ensure serial execution of the act() method.
     */
    private ControlConnector controlConnector = new ControlConnector();

    public void start() {
        if (started) {
            return;
        }
        started = true;
        controlConnector.turnOn();
    }

    public void stop() {
        stopped = true;
        controlConnector.turnOff();
    }

    public synchronized void consumeTokens() {
        connectors.forEach(connector -> connector.purge());
    }

    @Override
    public void run() {
        try {
            controlConnector.turnOff();
            act();
            if (stopped) {
                return;
            }
            consumeTokens();
            controlConnector.turnOn();
        } catch (Throwable e) {
            stop();
            // TODO move to failed state
            System.err.println("Error in actor " + getClass().getName());
            e.printStackTrace();
        }
    }

    class ControlConnector extends Connector {
    }
}