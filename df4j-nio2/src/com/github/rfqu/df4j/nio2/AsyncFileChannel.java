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
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.ClosedChannelException;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.util.HashSet;
import java.util.Set;

import com.github.rfqu.df4j.core.Port;
import com.github.rfqu.df4j.core.Task;

public class AsyncFileChannel {
    
    AsynchronousFileChannel channel=null;
    boolean closed=false;

    public AsyncFileChannel(Path file, Set<? extends OpenOption> options, FileAttribute<?>... attrs) throws IOException {
        close();
//        channel=AsynchronousFileChannel.open(file, options, Task.getCurrentExecutorService(), attrs);
        closed=false;
    }

    public AsyncFileChannel(Path file, OpenOption... options) throws IOException {
        HashSet<OpenOption> options2 = new HashSet<OpenOption>();
        for (OpenOption opt: options) {
            options2.add(opt);
        }
        close();
        channel=AsynchronousFileChannel.open(file, options2,
                Task.getCurrentExecutorService(), new FileAttribute<?>[0]);
        closed=false;
    }

    public <R extends FileIORequest> R read(R request, long position, Port<R> replyTo) throws Exception { 
        checkState();
        request.prepare(this, true, position, replyTo);
        channel.read(request.buffer, request.getPosition(), this, request);
        return request;
    }
    
    public FileIORequest read(ByteBuffer buf, long position, Port<FileIORequest> replyTo) throws Exception { 
        return read(new FileIORequest(buf), position, replyTo);
    }
    
    public <R extends FileIORequest> R write(R request, long position, Port<R> replyTo) throws Exception { 
        checkState();
        request.prepare(this, true, position, replyTo);
        channel.write(request.buffer, request.getPosition(), this, request);
        return request;
    }

    public FileIORequest write(ByteBuffer buf, long position, Port<FileIORequest> replyTo) throws Exception {
        return write(new FileIORequest(buf), position, replyTo);
    }

    protected void checkState() throws ClosedChannelException {
        if (closed) {
            throw new ClosedChannelException();
        }
    }

    public void close() throws IOException {
        closed=true;
        if (channel!=null) {
            AsynchronousFileChannel ch = channel;
            channel=null;
            ch.close();
        }
    }

    public void truncate(long size) throws IOException {
        channel.truncate(size);
    }

    public void force(boolean b) throws IOException {
        channel.force(b);
    }

    public AsynchronousFileChannel getChannel() {
        return channel;
    }
}
