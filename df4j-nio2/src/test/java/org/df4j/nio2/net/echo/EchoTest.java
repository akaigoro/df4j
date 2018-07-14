package org.df4j.nio2.net.echo;

import org.df4j.core.connector.messagescalar.CompletablePromise;
import org.df4j.core.node.Action;
import org.df4j.core.node.messagestream.Actor1;
import org.df4j.core.node.messagestream.PickPoint;
import org.df4j.core.util.Logger;
import org.df4j.nio2.net.ClientConnection;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * ConnectionSource => AsyncServerSocketChannel -> ServerConnection -|
 *     ^                                                             v
 *     |<------------------------------------------------------------|                                                             |
 */
public  class EchoTest {
    static final int BUF_SIZE = 128;
    static final SocketAddress local9990 = new InetSocketAddress("localhost", 9990);
    static final Charset charset = Charset.defaultCharset();

    ConnectionManager connectionManager;

    @Before
    public void init() throws IOException {
        connectionManager = new ConnectionManager(local9990,2);
        connectionManager.start();
    }

    @After
    public void close() {
        connectionManager.close();
    }
    
    /**
     * send a message from client to server 
     */
    @Test
    public void smokeIOTest() throws IOException, InterruptedException, ExecutionException, TimeoutException {
        String message="hi there";
        ClientConnection clientConn = new ClientConnection(local9990);
        ByteBuffer buf=ByteBuffer.allocate(BUF_SIZE);
        byte[] src = message.getBytes(charset);
        buf.put(src);
        clientConn.writer.input.post(buf);
        PickPoint<ByteBuffer> collector = new PickPoint<>();
        clientConn.reader.input.post(buf);
        clientConn.writer.output.subscribe(collector);
        ByteBuffer b = collector.poll(1, TimeUnit.SECONDS);
        Assert.assertEquals(buf, b);
    }

    @Test
    public void ClientTest1() throws IOException, InterruptedException, ExecutionException, TimeoutException {
        Client client = new Client(local9990, 10);
        client.result.get(5, TimeUnit.SECONDS);
    }


    static class Client extends Actor1<ByteBuffer> {
        protected static final Logger LOG = Logger.getLogger(Client.class.getName());

        CompletablePromise<Void> result = new CompletablePromise<>();
        String message="hi there";
        ClientConnection clientConn;
        ByteBuffer buf;
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
        public Client(SocketAddress addr, int count) throws IOException, InterruptedException {
            this.count = count;
            clientConn = new ClientConnection(addr);
            clientConn.writer.output.subscribe(this);
            buf = ByteBuffer.allocate(128);
            byte[] src = message.getBytes(charset);
            buf.put(src);
            clientConn.writer.input.post(buf);
        }

        @Action
        public void onBufRead(ByteBuffer b) {
            String m2 = new String(b.array(), charset);
            LOG.info("client received message:"+m2);
            Assert.assertEquals(message, m2);
            count--;
            if (count==0) {
                LOG.info("client finished successfully");
                result.complete(null);
                stop();
                return;
            }
            clientConn.writer.input.post(b);
        }
    }


}