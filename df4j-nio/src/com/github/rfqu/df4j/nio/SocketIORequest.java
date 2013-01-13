/* Copyright 2011-2012 by Alexei Kaigorodov
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
package com.github.rfqu.df4j.nio;

import java.nio.ByteBuffer;

/**
 * Request for a socket I/O operation.
 */
public class SocketIORequest<R extends SocketIORequest<R>>
  extends IORequest<R>
{
	private long timeout; // milliseconds
	private boolean timed;

    public SocketIORequest(ByteBuffer buf) {
        super(buf);
    }

    @Override
    public void prepareRead() {
        prepareRead(0);
    }

    @Override
    public void prepareWrite() {
        prepareWrite(0);
    }

    public void prepareRead(long timeout) {
        super.prepareRead();
        setTimed(false);
        this.setTimeout(timeout);
    }

    public void prepareWrite(long timeout) {
        super.prepareWrite();
        setTimed(false);
        this.setTimeout(timeout);
    }

    public boolean isTimed() {
        return timed;
    }

    public void setTimed(boolean timed) {
        this.timed = timed;
    }

    public long getTimeout() {
        return timeout;
    }

    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }

}
 