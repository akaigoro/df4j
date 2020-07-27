package org.df4j.nio2.net.echo;

import org.df4j.core.dataflow.Actor;
import org.df4j.core.dataflow.Dataflow;
import org.df4j.core.util.Logger;
import org.df4j.nio2.net.ReadBuffers;
import org.df4j.nio2.net.AsyncClientSocketChannel;
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

    protected AsyncClientSocketChannel inp = new AsyncClientSocketChannel(this);
    protected ReadBuffers clientConn;
    
    protected final Logger LOG = new Logger(this, Level.INFO);
    public int count;
    String message;
    private String clientName = "Client#"+seqNum;
    AsynchronousSocketChannel assc;

    public EchoClient(Dataflow dataflow, SocketAddress addr, int total) throws IOException {
        super(dataflow);
        this.count = total;
        inp.connect(addr);
    }

    public void runAction() {
        assc = inp.current();
        clientConn = new ReadBuffers(this, assc);
        clientConn.setName("client");
        LOG.info(clientName+" started");
        sendMsg(ByteBuffer.allocate(BUF_SIZE));
        nextAction(this::receiveMessage);
    }

    public static void toByteBuf(ByteBuffer buffer, String message) {
        buffer.clear();
        byte[] bytes = message.getBytes(charset);
        buffer.put(bytes);
        buffer.flip();
    }

    public static String fromByteBuf(ByteBuffer buffer) {
        buffer.flip();
        byte[] bytes = new byte[buffer.limit()];
        buffer.get(bytes);
        return new String(bytes, charset);
    }

    public void sendMsg(ByteBuffer buffer) {
        message = "hi there "+count;
        toByteBuf(buffer, message);
        clientConn.writer.input.onNext(buffer);
        LOG.info(clientName+" sent message: "+message);
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
        String m2 = fromByteBuf(received);
        LOG.info(clientName+" received message:"+m2);
        Assert.assertEquals(message, m2);
        if (count > 0) {
            count--;
            sendMsg(received);
        } else {
            try {
                assc.close();
                complete();
                LOG.info(clientName+" finished successfully");
            } catch (IOException e) {
                LOG.info(clientName+" finished exceptionally ("+e+")");
                completeExceptionally(e);
            }
        }
    }
}
