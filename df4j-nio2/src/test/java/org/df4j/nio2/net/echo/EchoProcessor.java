package org.df4j.nio2.net.echo;

import org.df4j.core.dataflow.Actor;
import org.df4j.core.dataflow.Dataflow;
import org.df4j.core.port.InpFlow;
import org.df4j.core.port.OutFlow;
import org.df4j.nio2.net.AsyncSocketChannel;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.concurrent.CompletionException;

class EchoProcessor extends Actor {
    private final EchoServer echoServer;
    AsyncSocketChannel serverConn;
    Long connSerialNum;
    InpFlow<ByteBuffer> readBuffers = new InpFlow<>(this);
    OutFlow<ByteBuffer> buffers2write = new OutFlow<>(this);;
    private boolean connectionPermitReleased;

    public EchoProcessor(EchoServer echoServer, Dataflow parent, AsyncSocketChannel assc, Long connSerialNum) {
        super(parent);
        this.echoServer = echoServer;
        this.connSerialNum = connSerialNum;
        int capacity = 2;
        serverConn.setName("server");
        for (int k = 0; k < capacity; k++) {
            ByteBuffer buf = ByteBuffer.allocate(EchoServer.BUF_SIZE);
            serverConn.reader.input.onNext(buf);
        }
        serverConn.reader.output.subscribe(readBuffers);
        buffers2write.subscribe(serverConn.writer.input);
        serverConn.writer.output.subscribe(serverConn.reader.input);
        echoServer.LOG.info("EchoProcessor #" + connSerialNum + " started");
    }

    public synchronized void releaseConnectionPermit() {
        if (connectionPermitReleased) {
            return;
        }
        connectionPermitReleased = true;
        serverConn.release(1);
    }

    @Override
    public void complete() {
        releaseConnectionPermit();
        super.complete();
    }

    @Override
    public void completeExceptionally(Throwable ex) {
        releaseConnectionPermit();
        super.completeExceptionally(ex);
    }

    public void runAction() throws CompletionException {
        if (!readBuffers.isCompleted()) {
            ByteBuffer buffer = readBuffers.remove();
            buffer.flip();
            buffers2write.onNext(buffer);
            echoServer.LOG.info("EchoProcessor #" + connSerialNum + " replied");
        } else {
            try {
                serverConn.close();
                complete();
                echoServer.LOG.info("EchoProcessor #" + connSerialNum + "completed");
            } catch (IOException e) {
                completeExceptionally(e);
                echoServer.LOG.info("EchoProcessor #" + connSerialNum + "completed with error " + e);
            }
        }
    }
}
