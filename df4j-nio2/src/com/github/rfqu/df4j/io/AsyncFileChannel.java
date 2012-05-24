package com.github.rfqu.df4j.io;

import java.io.IOException;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.ClosedChannelException;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.util.HashSet;
import java.util.Set;

import com.github.rfqu.df4j.core.Task;

public class AsyncFileChannel {
    
    AsynchronousFileChannel channel=null;
    boolean closed=false;

    public AsyncFileChannel(Path file, Set<? extends OpenOption> options, FileAttribute<?>... attrs) throws IOException {
        close();
        channel=AsynchronousFileChannel.open(file, options, Task.getCurrentExecutorService(), attrs);
        closed=false;
    }

    public AsyncFileChannel(Path file, OpenOption... options) throws IOException {
        HashSet<OpenOption> options2 = new HashSet<OpenOption>();
        for (OpenOption opt: options) {
            options2.add(opt);
        }
        close();
        channel=AsynchronousFileChannel.open(file, options2, Task.getCurrentExecutorService(), new FileAttribute<?>[0]);
        closed=false;
    }

    public void read(FileIORequest request, long position) throws Exception { 
        checkRequest(request);
        request.startRead(position);
        channel.read(request.buffer, position, this, request);
    }
    
    public void write(FileIORequest request, long position) throws Exception {
        checkRequest(request);
        request.startWrite(position);
        channel.write(request.buffer, position, this, request);
    }

    protected void checkRequest(FileIORequest request) throws ClosedChannelException {
        if (request==null) {
            throw new IllegalArgumentException("request==null");
        }
        if (channel==null) {
            throw new IllegalStateException("channel not opened");
        }
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
