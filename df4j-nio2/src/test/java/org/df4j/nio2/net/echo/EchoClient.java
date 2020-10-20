package org.df4j.nio2.net.echo;

import org.df4j.core.actor.Actor;
import org.df4j.core.actor.Dataflow;
import org.df4j.core.util.Logger;
import org.df4j.nio2.net.SocketPort;
import org.df4j.nio2.net.ClientSocketPort;
import org.junit.Assert;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletionException;
import java.util.logging.Level;

import static org.df4j.nio2.net.echo.EchoServer.BUF_SIZE;

/**
 * sends and receives limited number of messages
 *
 * demonstrates dynamic port creation {@link #clientConn}
 */
class EchoClient extends Actor {
    static final Charset charset = StandardCharsets.UTF_8;
    protected final Logger LOG = new Logger(this, Level.INFO);

    protected ClientSocketPort inp = new ClientSocketPort(this);
    protected SocketPort clientConn;
    
    public int count;
    String message;
    private String clientName = "Client#"+seqNum;

    public EchoClient(Dataflow dataflow, SocketAddress addr, int total) throws IOException {
        super(dataflow);
        this.count = total;
        inp.connect(addr);
    }

    public static void toByteBuf(ByteBuffer buffer, String message) {
        buffer.clear();
        byte[] bytes = message.getBytes(charset);
        buffer.put(bytes);
        buffer.flip();
    }

    public static String fromByteBuf(ByteBuffer buffer) {
        byte[] bytes = new byte[buffer.limit()];
        buffer.get(bytes);
        return new String(bytes, charset);
    }

    public void sendMsg(ByteBuffer buffer) {
        message = "hi there "+count;
        toByteBuf(buffer, message);
        clientConn.send(buffer);
        LOG.info(clientName+" sent message: "+message);
    }

    public void runAction() {
        AsynchronousSocketChannel assc = inp.current();
        clientConn = new SocketPort(this);
        clientConn.setName("client");
        clientConn.connect(assc);
        LOG.info(clientName+" started");
        sendMsg(ByteBuffer.allocate(BUF_SIZE));
        nextAction(this::receiveMessage);
    }

    public void receiveMessage() throws CompletionException {
        if (clientConn.isCompleted()) {
            if (clientConn.isCompletedExceptionally()) {
                completeExceptionally(clientConn.getCompletionException());
            } else {
                complete();
            }
            return;
        }
        ByteBuffer received = clientConn.remove();
        received.flip();
        String m2 = fromByteBuf(received);
        LOG.info(clientName+" received message:"+m2);
        Assert.assertEquals(message, m2);
        count--;
        sendMsg(received);
        if (count == 0) {
            complete();
        }
    }

    @Override
    protected void whenComplete() {
        try {
            inp.cancel();
            LOG.info(clientName+" finished successfully");
        } catch (IOException e) {
            LOG.info(clientName+" finished exceptionally ("+e+")");
            completeExceptionally(e);
        }
    }
}
