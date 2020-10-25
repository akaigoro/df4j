package org.df4j.nio2.net.echo;

import org.df4j.core.actor.Actor;
import org.df4j.core.actor.ActorGroup;
import org.df4j.core.port.InpFlow;
import org.df4j.core.util.LoggerFactory;
import org.df4j.nio2.net.AsyncClientSocketChannel;
import org.df4j.nio2.net.AsyncSocketChannel;
import org.junit.Assert;
import org.slf4j.Logger;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.charset.Charset;

import static org.df4j.nio2.net.echo.EchoServer.BUF_SIZE;

/**
 * sends and receives limited number of messages
 *
 * demonstrates dynamic port creation {@link #readBuffers}
 */
class EchoClient extends Actor {
    static final Charset charset = Charset.forName("UTF-8");

    protected final Logger LOG = LoggerFactory.getLogger(this);
    public int count;
    protected AsyncClientSocketChannel inp = new AsyncClientSocketChannel(this);
    AsyncSocketChannel clientConn;
    InpFlow<ByteBuffer> readBuffers;
    String message;
    private String clientName;
    AsynchronousSocketChannel assc;

    public EchoClient(ActorGroup dataflow, SocketAddress addr, int seqNum, int total) throws IOException {
        super(dataflow);
        clientName = "Client#"+seqNum;
        this.count = total;
        inp.connect(addr);
    }

    public void runAction() {
        assc = inp.current();
        clientConn = new AsyncSocketChannel(getActorGroup(), assc);
        clientConn.setName("client");
        readBuffers = new InpFlow<>(this);
        clientConn.writer.output.subscribe(clientConn.reader.input);
        clientConn.reader.output.subscribe(readBuffers);
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

    public void receiveMessage() {
        ByteBuffer received = readBuffers.remove();
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
