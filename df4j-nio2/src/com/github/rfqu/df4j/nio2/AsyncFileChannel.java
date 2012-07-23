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
package com.github.rfqu.df4j.nio2;

import java.io.IOException;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.CompletionHandler;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import com.github.rfqu.df4j.core.Port;
import com.github.rfqu.df4j.core.Task;

public class AsyncFileChannel
  implements CompletionHandler<Integer, FileIORequest<?>>
{
    
    AsynchronousFileChannel channel=null;
    boolean closed=false;

    public AsyncFileChannel(Path file, Set<? extends OpenOption> options, FileAttribute<?>... attrs) throws IOException {
        channel=AsynchronousFileChannel.open(file, options, Task.getCurrentExecutorService(), attrs);
    }

    public AsyncFileChannel(Path file, OpenOption... options) throws IOException {
        this(file, new HashSet<OpenOption>(Arrays.<OpenOption>asList(options)), new FileAttribute<?>[0]);
    }

    public <R extends FileIORequest<R>> R read(R request, long position, Port<R> replyTo) throws Exception { 
        checkState();
        request.prepare(true, position, replyTo);
        channel.read(request.buffer, request.getPosition(), request, this);
        return request;
    }
    
    public <R extends FileIORequest<R>> R write(R request, long position, Port<R> replyTo) throws Exception { 
        checkState();
        request.prepare(false, position, replyTo);
        channel.write(request.buffer, request.getPosition(), request, this);
        return request;
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

    @Override
    public void completed(Integer result, FileIORequest<?> attachment) {
        attachment.completed(result);
    }

    @Override
    public void failed(Throwable exc, FileIORequest<?> attachment) {
        attachment.failed(exc);
    }
}
