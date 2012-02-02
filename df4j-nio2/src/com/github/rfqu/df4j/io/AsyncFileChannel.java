package com.github.rfqu.df4j.io;

import java.io.IOException;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import com.github.rfqu.df4j.core.Actor;

public class AsyncFileChannel {

    public static AsynchronousFileChannel open(Path file, Set<? extends OpenOption> options, FileAttribute<?>... attrs) throws IOException {
        ExecutorService executor=Actor.getCurrentExecutor();
        return AsynchronousFileChannel.open(file, options, executor, attrs);
    }

    public static AsynchronousFileChannel open(Path file, OpenOption... options) throws IOException {
        ExecutorService executor=Actor.getCurrentExecutor();
        HashSet<OpenOption> options2 = new HashSet<OpenOption>();
        for (OpenOption opt: options) {
            options2.add(opt);
        }
        return AsynchronousFileChannel.open(file, options2, executor, new FileAttribute<?>[0]);
    }

}
