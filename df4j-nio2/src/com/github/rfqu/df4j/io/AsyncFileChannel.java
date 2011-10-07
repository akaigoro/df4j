package com.github.rfqu.df4j.io;

import java.io.IOException;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.CompletionHandler;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import com.github.rfqu.df4j.core.Task;

public class AsyncFileChannel {

    public static AsynchronousFileChannel open(Path file, Set<? extends OpenOption> options, FileAttribute<?>... attrs) throws IOException {
        ExecutorService executor=Task.getCurrentExecutor();
        return AsynchronousFileChannel.open(file, options, executor, attrs);
    }

    public static AsynchronousFileChannel open(Path file, OpenOption... options) throws IOException {
        ExecutorService executor=Task.getCurrentExecutor();
        HashSet<OpenOption> options2 = new HashSet<OpenOption>();
        for (OpenOption opt: options) {
            options2.add(opt);
        }
        return AsynchronousFileChannel.open(file, options2, executor, new FileAttribute<?>[0]);
    }

    protected AsynchronousFileChannel channel;
    
    public AsyncFileChannel(AsynchronousFileChannel channel) {
        this.channel = channel;
    }

    public void read(FileIORequest request, long filePosition) {
        request.start(this, filePosition, true);
        channel.read(request.buffer, filePosition, request, requestCompletion);
    }

    public void write(FileIORequest request, long filePosition) {
        request.start(this, filePosition, false);
        channel.write(request.buffer, filePosition, request, requestCompletion);
    }

    protected void requestCompleted(FileIORequest request) {
    }

    static final CompletionHandler<Integer, FileIORequest> requestCompletion =new CompletionHandler<Integer, FileIORequest>() {
        @Override
        public void completed(Integer result, FileIORequest request) {
            request.completed(result);
        }
        @Override
        public void failed(Throwable exc, FileIORequest request) {
            request.failed(exc);
        }
    };
}
