package org.df4j.nio2.net.echo;

import org.df4j.core.actor.Actor;
import org.df4j.core.port.InpMessage;
import org.df4j.core.util.Logger;
import org.df4j.nio2.net.ClientConnection;
import org.junit.Assert;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

/** receives sends and receives limited number of messages
 *
 */
class EchoClient extends Actor {
    protected static final Logger LOG = Logger.getLogger(EchoClient.class.getName());
    static final Charset charset = Charset.forName("UTF-16");

    public static ByteBuffer toByteBuf(String message) {
        return ByteBuffer.wrap(message.getBytes(charset));
    }

    public static String fromByteBuf(ByteBuffer b) {
        return new String(b.array(), charset);
    }

    InpMessage<ByteBuffer> buffers = new InpMessage<>(this);
    String message;
    ClientConnection clientConn;
    int count;

    public EchoClient(SocketAddress addr, int count) throws IOException, InterruptedException {
        this.count = count;
        this.message= "hi there "+count;
        clientConn = new ClientConnection("Client", addr);
        clientConn.writer.output.subscribe(buffers);
        ByteBuffer buf = toByteBuf(message);
        buffers.onNext(buf);
    }

    public void runAction() {
        ByteBuffer b = buffers.remove();
        String m2 = fromByteBuf(b);
        LOG.info("client received message:"+m2);
        Assert.assertEquals(message, m2);
        if (count > 0) {
            count--;
            buffers.onNext(b);
        } else  {
            LOG.info("client finished successfully");
            clientConn.close();
            stop();
        }
    }

}
