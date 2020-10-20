package org.df4j.nio2.net.echo;

import org.df4j.core.actor.Actor;
import org.df4j.core.actor.Dataflow;
import org.df4j.core.util.Logger;
import org.df4j.nio2.net.Connection;
import org.df4j.nio2.net.SocketPort;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletionException;
import java.util.logging.Level;

class EchoProcessor extends Actor {
    protected final Logger LOG = new Logger(this, Level.INFO);
    Long connSerialNum;
    protected SocketPort socketPort;
    public String name;

    public EchoProcessor(Dataflow parent, Connection channel, Long connSerialNum) {
        super(parent);
        this.connSerialNum = connSerialNum;
        int capacity = 2;
        socketPort = new SocketPort(this);
        name = " processor#"+connSerialNum;
        socketPort.connect(channel);
        for (int k = 0; k < capacity; k++) {
            ByteBuffer buf = ByteBuffer.allocate(EchoServer.BUF_SIZE);
            socketPort.read(buf);
        }
        LOG.info(name+" started");
    }

    @Override
    public String toString() {
        if (name == null) {
            return super.toString();
        } else {
            return name;
        }
    }

    @Override
    public void whenComplete() {
        try {
            socketPort.close();
            LOG.info(name + "completed");
        } catch (IOException e) {
            completeExceptionally(e);
            LOG.info(name + "completed with error " + e);
        }
    }

    public void runAction() throws CompletionException {
        if (socketPort.isCompleted()) {
            complete();
            return;
        }
        ByteBuffer received = socketPort.remove();
        received.flip();
        String m2 = EchoClient.fromByteBuf(received);
        socketPort.send(received);
        LOG.info(name + " received "+m2);
    }
}
