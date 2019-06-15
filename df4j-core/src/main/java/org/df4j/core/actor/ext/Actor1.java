/*
 * Copyright 2011 by Alexei Kaigorodov
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.df4j.core.actor.ext;

import org.df4j.core.actor.StreamInput;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

/**
 * A dataflow Actor with one predefined input stream port.
 * It mimics the Actors described by Carl Hewitt.
 * This class, however, still can have other (named) ports.
 *
 * @param <M> the type of messages, accepted via predefined port.
 */
public abstract class Actor1<M> extends FancyActor implements Subscriber<M> {
    protected final StreamInput<M> mainInput;

    public Actor1(int capacity) {
        mainInput = new StreamInput<M>(this, capacity);
    }

    public Actor1() {
        this(16);
    }

    @Override
    public void onSubscribe(Subscription s) {
        mainInput.onSubscribe(s);
    }

    @Override
    public void onNext(M m) {
        mainInput.onNext(m);
    }

    @Override
    public void onError(Throwable ex) {
        mainInput.onError(ex);
    }

    /**
     * processes closing signal
     */
    @Override
    public void onComplete() {
        mainInput.onComplete();
    }

    @Override
    protected void runAction() throws Exception {
        M message = mainInput.current();
        if (message != null) {
            runAction(message);
        } else {
            completion(mainInput.getCompletionException());
        }
    }

    protected abstract void runAction(M arg) throws Exception;

    protected void completion(Throwable completionException) throws Exception {
        stop();
    }
}
