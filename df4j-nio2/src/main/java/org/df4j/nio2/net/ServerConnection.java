/*
 * Copyright 2011-2012 by Alexei Kaigorodov
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions
 * and limitations under the License.
 */
package org.df4j.nio2.net;

import org.df4j.core.connector.messagescalar.ScalarCollector;

import java.nio.channels.AsynchronousSocketChannel;

/**
 * Wrapper over {@link AsynchronousSocketChannel}.
 * Simplifies input-output, handling queues of I/O requests.
 * 
 * Internally, manages 2 input queues: one for reading requests and one for writing requests.
 * After request is served, it is sent to the port denoted by <code>replyTo</code>
 * property in the request.
 * 
 * IO requests can be posted immediately, but will be executed
 * only after connection completes.
 * If interested in the moment when connection is established,
 * add a listener to connEvent.
 */
public class ServerConnection extends AsyncSocketChannel {

    public ServerConnection(String name, ScalarCollector backport) {
        super(name, backport);
    }

}
