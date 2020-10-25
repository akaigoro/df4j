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

import org.df4j.core.port.InpSignal;

import java.io.IOException;
import java.nio.channels.AsynchronousSocketChannel;

/**
 * Wrapper over {@link AsynchronousSocketChannel}.
 * Simplifies input-output, handling queues of I/O requests.
 */
public class Connection {
    private final Long connSerialNum;
    private final InpSignal allowedConnections;
    private volatile AsynchronousSocketChannel channel;
    private boolean completed = false;

    public Connection(AsynchronousSocketChannel channel, Long connSerialNum, InpSignal allowedConnections) {
        this.connSerialNum = connSerialNum;
        this.allowedConnections = allowedConnections;
        this.channel=channel;
    }

    public Connection(AsynchronousSocketChannel assc, Long connSerialNum) {
        this(assc, connSerialNum, null);
    }

    public AsynchronousSocketChannel getChannel() {
        return channel;
    }

    public Long getConnSerialNum() {
        return connSerialNum;
    }

    /**
     *
     * @throws IOException
     *          If an I/O error occurs
     */
    public synchronized void close() throws IOException {
        if (completed) {
            return;
        }
        allowedConnections.release();
        channel.close();
        completed = true;
    }
}
