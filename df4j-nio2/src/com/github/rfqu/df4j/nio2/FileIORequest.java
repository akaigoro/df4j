/*
 * Copyright 2011 by Alexei Kaigorodov
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package com.github.rfqu.df4j.nio2;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousCloseException;

import com.github.rfqu.df4j.core.Port;

//public class FileIORequest<R extends IORequest<R, AsyncFileChannel>> extends IORequest<R, AsyncFileChannel> {
public class FileIORequest<R extends FileIORequest<R>> extends IORequest<R, AsyncFileChannel> {
    private long position;
    
    public FileIORequest(ByteBuffer buf) {
        super(buf);
    }

    public void prepare(AsyncFileChannel channel,
            boolean read, long position, Port<R> replyTo)
    {
        super.prepare(channel, read, replyTo);
        this.position = position;
    }

    @Override
    public void failed(Throwable exc, AsyncFileChannel channel) {
        if (exc instanceof AsynchronousCloseException) {
            try {
                channel.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        super.failed(exc, channel);
    }

    public long getPosition() {
        return position;
    }

}
