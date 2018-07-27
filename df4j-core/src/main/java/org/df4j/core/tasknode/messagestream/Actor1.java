/*
 * Copyright 2011 by Alexei Kaigorodov
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.df4j.core.tasknode.messagestream;

import org.df4j.core.boundconnector.messagescalar.SimpleSubscription;
import org.df4j.core.boundconnector.messagestream.StreamSubscriber;
import org.df4j.core.boundconnector.messagestream.StreamInput;

/**
 * A dataflow Actor with one predefined input stream port.
 * It mimics the Actors described by Carl Hewitt.
 * This class, however, still can have other (named) ports.
 * @param <M> the type of messages, accepted via predefined port.
 */
public abstract class Actor1<M> extends Actor implements StreamSubscriber<M> {
    protected final StreamInput<M> mainInput = new StreamInput<M>(this);

    @Override
    public synchronized void onSubscribe(SimpleSubscription subscription) {
        mainInput.onSubscribe(subscription);
    }

    @Override
    public void post(M m) {
        mainInput.post(m);
    }

    @Override
    public void postFailure(Throwable ex) {
        mainInput.postFailure(ex);
    }

    /**
     * processes closing signal
     * @throws Exception
     */
    @Override
    public void complete() {
        mainInput.complete();
    }

    public boolean isClosed() {
        return mainInput.isClosed();
    }

    @Override
    protected Void runAction() throws Exception {
        M arg = mainInput.next();
        runAction(arg);
        return null;
    }

    protected abstract void runAction(M arg) throws Exception;
}
