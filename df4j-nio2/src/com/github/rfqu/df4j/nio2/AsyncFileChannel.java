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
import com.github.rfqu.df4j.core.DFContext;

/**
 * Wrapper over {@link java.nio.channels.AsynchronousFileChannel}.
 * @author rfqu
 *
 */
public class AsyncFileChannel<T extends FileIORequest<T>> 
  implements Port<T>,
     CompletionHandler<Integer, T>
{    
    AsynchronousFileChannel channel=null;
    boolean closed=false;

    public AsyncFileChannel(Path file, Set<? extends OpenOption> options, FileAttribute<?>... attrs) throws IOException {
        channel=AsynchronousFileChannel.open(file, options, DFContext.getCurrentExecutorService(), attrs);
    }

    public AsyncFileChannel(Path file, OpenOption... options) throws IOException {
        this(file, new HashSet<OpenOption>(Arrays.<OpenOption>asList(options)), new FileAttribute<?>[0]);
    }

    @Override
    public void post(T request) { 
		if (closed) {
			request.postFailure(new ClosedChannelException());
			return;
		}
        if (request.isReadOp()) {
        	channel.read(request.getBuffer(), request.getPosition(), request, this);
        } else {
            channel.write(request.getBuffer(), request.getPosition(), request, this);
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
    public void completed(Integer result, T attachment) {
        attachment.post(result);
    }

    @Override
    public void failed(Throwable exc, T attachment) {
        attachment.postFailure(exc);
    }
}
