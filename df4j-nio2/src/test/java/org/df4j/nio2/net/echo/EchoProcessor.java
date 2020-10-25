package org.df4j.nio2.net.echo;

import org.df4j.core.actor.Actor;
import org.df4j.core.actor.ActorGroup;
import org.df4j.core.port.InpFlow;
import org.df4j.core.port.OutFlow;
import org.df4j.nio2.net.AsyncSocketChannel;
import org.df4j.nio2.net.Connection;

import java.io.IOException;
import java.nio.ByteBuffer;

class EchoProcessor extends Actor {
    private final EchoServer echoServer;
    private final String name;
    private final Connection conn;
    AsyncSocketChannel serverConn;
    InpFlow<ByteBuffer> readBuffers = new InpFlow<>(this);
    OutFlow<ByteBuffer> buffers2write = new OutFlow<>(this);

    public EchoProcessor(EchoServer echoServer, ActorGroup parent, Connection conn) {
        super(parent);
        this.echoServer = echoServer;
        this.conn = conn;
        int capacity = 2;
        serverConn = new AsyncSocketChannel(getActorGroup(), conn.getChannel());
        serverConn.setName("server");
        name = "EchoProcessor #" + conn.getConnSerialNum();
        serverConn.reader.input.setCapacity(capacity);
        for (int k = 0; k < capacity; k++) {
            ByteBuffer buf = ByteBuffer.allocate(EchoServer.BUF_SIZE);
            serverConn.reader.input.onNext(buf);
        }
        serverConn.reader.output.subscribe(readBuffers);
        buffers2write.subscribe(serverConn.writer.input);
        serverConn.writer.output.subscribe(serverConn.reader.input);
        echoServer.LOG.info(name + " started");
    }

    @Override
    public void whenComplete() {
        try {
            conn.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void whenError(Throwable ex) {
        try {
            conn.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void runAction() {
        if (!readBuffers.isCompleted()) {
            ByteBuffer buffer = readBuffers.remove();
            buffer.flip();
            buffers2write.onNext(buffer);
            echoServer.LOG.info(name + " replied");
        } else {
            try {
                serverConn.close();
                complete();
                echoServer.LOG.info(name + "completed");
            } catch (IOException e) {
                completeExceptionally(e);
                echoServer.LOG.info(name + "completed with error " + e);
            }
        }
    }
}
