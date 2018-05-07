/*
 * Copyright 2011 by Alexei Kaigorodov
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.df4j.core.node.messagescalar;

import org.df4j.core.connector.messagescalar.ScalarPublisher;
import org.df4j.core.connector.messagescalar.ScalarSubscriber;
import org.df4j.core.connector.messagestream.StreamInput;
import org.df4j.core.util.SameThreadExecutor;
import org.df4j.core.node.Actor;

public abstract class AbstractPromise<M> extends Actor implements ScalarPublisher<M> {
    /** place for input token(s) */
    protected final StreamInput<ScalarSubscriber<? super M>> requests = new StreamInput<>(this);

    {
        setExecutor(new SameThreadExecutor());
        start();
    }

    @Override
    public <S extends ScalarSubscriber<? super M>> S subscribe(S subscriber) {
        requests.post(subscriber);
        return subscriber;
    }

    @Override
    protected void act() {
        ScalarSubscriber<? super M> request = this.requests.current();
        M result = getToken();
        request.post(result);
    }

    protected abstract M getToken();

}
