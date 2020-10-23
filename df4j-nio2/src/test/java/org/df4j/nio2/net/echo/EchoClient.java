package org.df4j.nio2.net.echo;

import org.df4j.core.actor.Actor;
import org.df4j.core.actor.ActorGroup;
import org.df4j.core.util.LoggerFactory;
import org.df4j.nio2.net.SocketPort;
import org.df4j.nio2.net.ClientSocketPort;
import org.junit.Assert;
import org.slf4j.Logger;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletionException;

import static org.df4j.nio2.net.echo.EchoServer.BUF_SIZE;

/**
 * sends and receives limited number of messages
 *
 * demonstrates dynamic port creation {@link #clientConn}
 */
class EchoClient extends Actor {
    static final Charset charset = StandardCharsets.UTF_8;
    protected final Logger logger = LoggerFactory.getLogger(this);

    protected ClientSocketPort inp = new ClientSocketPort(this);
    protected SocketPort clientConn;
    
    public int count;
    String message;
    private String clientName = "Client#"+seqNum;

    public EchoClient(ActorGroup actorGroup, SocketAddress addr, int total) throws IOException {
        super(actorGroup);
        this.count = total;
        inp.connect(addr);
    }

    public EchoClient(SocketAddress addr, int total) throws IOException {
        this(new ActorGroup(), addr, total);
    }

    public EchoClient(int port, int total) throws IOException {
        this(new InetSocketAddress("localhost", port), total);
    }

    public static void toByteBuf(ByteBuffer buffer, String message) {
        buffer.clear();
        byte[] bytes = (message+'\n').getBytes(charset);
        buffer.put(bytes);
        buffer.flip();
    }

    public static String fromByteBuf(ByteBuffer buffer) {
        byte[] bytes = new byte[buffer.limit()];
        buffer.get(bytes);
        return new String(bytes, charset).replace("\n", "");
    }

    public void sendMsg(ByteBuffer buffer) {
        message = "hi there "+count;
        toByteBuf(buffer, message);
        clientConn.send(buffer);
        logger.info(clientName+" sent message: "+message);
    }

    public void runAction() {
        AsynchronousSocketChannel assc = inp.current();
        clientConn = new SocketPort(this);
        clientConn.setName("client");
        clientConn.connect(assc);
        logger.info(clientName+" started");
        sendMsg(ByteBuffer.allocate(BUF_SIZE));
        nextAction(this::receiveMessage);
    }

    public void receiveMessage() throws CompletionException {
        if (clientConn.isCompleted()) {
            if (clientConn.isCompletedExceptionally()) {
                completeExceptionally(clientConn.getCompletionException());
                logger.info(" clientConn completed with error");
            } else {
                complete();
                logger.info(" clientConn completed");
            }
            return;
        }
        ByteBuffer received = clientConn.remove();
        received.flip();
        String m2 = fromByteBuf(received);
        logger.info(clientName+" received message:"+m2);
        Assert.assertEquals(message, m2);
        sendMsg(received);
        if (--count == 0) {
            complete();
        }
    }

    @Override
    protected void whenComplete() {
        try {
            inp.cancel();
            logger.info(clientName+" finished successfully");
        } catch (IOException e) {
            logger.info(clientName+" finished exceptionally ("+e+")");
            completeExceptionally(e);
        }
    }

    public static void main(String... args) throws IOException, InterruptedException {
        int port = args.length==0?EchoTest.port:Integer.valueOf(args[0]);
        EchoClient client = new EchoClient(port, 10);
        EchoServer.MyExec exec = new EchoServer.MyExec();
        client.setExecutor(exec);
        exec.doAll();
    }
}
