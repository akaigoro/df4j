/*
 * Copyright 2011 by Alexei Kaigorodov
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package org.df4j.core.connector.messagescalar;

/**
 * scalar inlet for messages
 * @param <M> the type of the message
 */
@FunctionalInterface
public interface ScalarCollector<M> {

    /**
     * If this ScalarSubscriber was not already completed, sets it completed state.
     * @param message
     * @return true if this message caused this ScalarSubscriber instance to asyncTask to a completed state, else false
     */
    void post(M message);

	/**
     * If this ScalarSubscriber was not already completed, sets it completed state.
     * @param ex
     * @return true if this exception caused this ScalarSubscriber instance to asyncTask to a completed state, else false
     */
	default void postFailure(Throwable ex) {}

}
