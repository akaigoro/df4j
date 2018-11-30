/*
 * Copyright 2011 by Alexei Kaigorodov
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package org.df4j.core.boundconnector.messagescalar;

import java.util.function.BiConsumer;

/**
 * scalar inlet for messages
 * @param <M> the type of the message
 */
public interface ScalarCollector<M>
        extends BiConsumer<M, Throwable> // to connect to a CompletionStage by whenComplete
{

    /**
     * If this ScalarSubscriber was not already completed, sets it completed state.
     * @param message the completion value
     * @return true if this message caused this ScalarSubscriber instance to asyncTask to a completed state, else false
     */
    boolean complete(M message);

	/**
     * If this ScalarSubscriber was not already completed, sets it completed state.
     * @param ex the completion exception
     * @return true if this exception caused this ScalarSubscriber instance to asyncTask to a completed state, else false
     */
	default boolean completeExceptionally(Throwable ex) {return false;}

    @Override
    default void accept(M r, Throwable throwable) {
        if (throwable != null) {
            completeExceptionally(throwable);
        } else {
            complete(r);
        }
    }

}
