package org.df4j.nio2.net.echo;

import org.df4j.core.connector.messagescalar.CompletablePromise;
import org.df4j.core.node.Action;
import org.df4j.core.node.messagestream.Actor1;
import org.df4j.core.util.Logger;
import org.df4j.nio2.net.ClientConnection;
import org.junit.Assert;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;

class EchoClient extends Actor1<ByteBuffer> {
    protected static final Logger LOG = Logger.getLogger(EchoClient.class.getName());

    CompletablePromise<Void> result = new CompletablePromise<>();
    String message="hi there";
    ClientConnection clientConn;
    int count;

    @Override
    public void postFailure(Throwable ex) {
        result.completeExceptionally(ex);
    }

    /**
     * Starts connection to a server. IO requests can be queued immediately,
     * but will be executed only after connection completes.
     *
     * @param addr
     * @param i
     * @throws IOException
     */
    public EchoClient(SocketAddress addr, int count) throws IOException, InterruptedException {
        this.count = count;
        clientConn = new ClientConnection("Client", addr);
        clientConn.writer.output.subscribe(this);
        String message = this.message;
        ByteBuffer buf = Utils.toByteBuf(message);
        clientConn.writer.input.post(buf);
    }

    @Action
    public void onBufRead(ByteBuffer b) {
        String m2 = Utils.fromByteBuf(b);
        LOG.info("client received message:"+m2);
        Assert.assertEquals(message, m2);
        count--;
        if (count==0) {
            LOG.info("client finished successfully");
            result.complete(null);
            clientConn.close();
            stop();
            return;
        }
        clientConn.writer.input.post(b);
    }

}
