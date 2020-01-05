package org.df4j.nio2.net.echo;

import org.df4j.core.communicator.AsyncSemaphore;
import org.df4j.core.dataflow.Actor;
import org.df4j.core.dataflow.Dataflow;
import org.df4j.core.port.InpFlow;
import org.df4j.core.port.OutFlow;
import org.df4j.core.util.Logger;
import org.df4j.nio2.net.AsyncServerSocketChannel;
import org.df4j.nio2.net.AsyncSocketChannel;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.concurrent.CompletableFuture;

/**
 * generates {@link EchoServerConnection}s for incoming connections
 *
 */
public class EchoServer extends Actor {
    protected final Logger LOG = new Logger(this);
    CompletableFuture s;
    /** limits the munber of simultaneously existing connections */
    protected AsyncSemaphore allowedConnections = new AsyncSemaphore();
    protected InpFlow<AsynchronousSocketChannel> inp = new InpFlow<>(this);

    public EchoServer(SocketAddress addr) throws IOException {
        super(new Dataflow());
        AsyncServerSocketChannel serverStarter = new AsyncServerSocketChannel(getDataflow(), addr);
        serverStarter.start();
        serverStarter.out.subscribe(inp);
        allowedConnections.release(2);
    }

    public void close() {
        stop();
    }

    @Override
    public void runAction() {
        LOG.info("EchoServer#runAction");
        AsynchronousSocketChannel assc = inp.removeAndRequest();
        EchoProcessor processor = new EchoProcessor(assc);
        processor.start();
    }

    class EchoProcessor extends Actor {
        AsyncSocketChannel serverConn;
        InpFlow<ByteBuffer> readBuffers = new InpFlow<>(this);
        OutFlow<ByteBuffer> buffers2write = new OutFlow<>(this);

        public EchoProcessor(AsynchronousSocketChannel assc) {
            super(EchoServer.this.getDataflow());
            LOG.info("EchoProcessor#init");
            int capacity = 2;
            serverConn = new AsyncSocketChannel(getDataflow(), assc);
            serverConn.setBackPort(allowedConnections);
            serverConn.setName("server");
            serverConn.reader.input.setCapacity(capacity);
            for (int k = 0; k<capacity; k++) {
                ByteBuffer buf=ByteBuffer.allocate(128);
                serverConn.reader.input.onNext(buf);
            }
            serverConn.reader.output.subscribe(readBuffers);
            buffers2write.subscribe(serverConn.writer.input);
            serverConn.writer.output.subscribe(serverConn.reader.input);
        }

        public void runAction() {
            LOG.info("EchoProcessor#runAction");
            ByteBuffer b = readBuffers.removeAndRequest();
            buffers2write.onNext(b);
        }
    }
}
