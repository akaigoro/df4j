package org.df4j.nio2.net.echo;

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
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletionException;
import java.util.logging.Level;

/**
 * generates {@link EchoProcessor}s for incoming connections
 *
 */
public class EchoServer extends AsyncServerSocketChannel {
    public static final int BUF_SIZE = 128;
    protected final Logger LOG = new Logger(this, Level.INFO);
    Set<EchoProcessor> echoProcessors = new HashSet<>();

    public EchoServer(Dataflow dataflow, SocketAddress addr, int maxConnCount) throws IOException {
        super(dataflow, addr);
        allowedConnections.release(maxConnCount);
    }

    public void complete() {
        for (EchoProcessor processor: echoProcessors) {
            processor.complete();
        }
        super.complete();
    }

    @Override
    protected void onAccept(AsynchronousSocketChannel asc, Long connSerialNum) {
        EchoProcessor processor = new EchoProcessor(getParent(), asc, connSerialNum); // create client connection
        processor.start();
    }

    class EchoProcessor extends Actor {
        AsyncSocketChannel serverConn;
        Long connSerialNum;
        InpFlow<ByteBuffer> readBuffers = new InpFlow<>(this);
        OutFlow<ByteBuffer> buffers2write = new OutFlow<>(this);
        private boolean connectionPermitReleased;

        public EchoProcessor(Dataflow parent, AsynchronousSocketChannel assc, Long connSerialNum) {
            super(parent);
            this.connSerialNum = connSerialNum;
            int capacity = 2;
            serverConn = new AsyncSocketChannel(getParent(), assc);
            serverConn.setName("server");
            for (int k = 0; k<capacity; k++) {
                ByteBuffer buf=ByteBuffer.allocate(BUF_SIZE);
                serverConn.reader.input.onNext(buf);
            }
            serverConn.reader.output.subscribe(readBuffers);
            buffers2write.subscribe(serverConn.writer.input);
            serverConn.writer.output.subscribe(serverConn.reader.input);
            LOG.info("EchoProcessor #"+connSerialNum+" started");
        }

        public synchronized void releaseConnectionPermit() {
            if (connectionPermitReleased) {
                return;
            }
            connectionPermitReleased = true;
            allowedConnections.release(1);
        }

        @Override
        public void complete() {
            releaseConnectionPermit();
            super.complete();
        }

        @Override
        public void completeExceptionally(Throwable ex) {
            releaseConnectionPermit();
            super.completeExceptionally(ex);
        }

        public void runAction() throws CompletionException {
            if (!readBuffers.isCompleted()) {
                ByteBuffer buffer = readBuffers.remove();
                buffer.flip();
                buffers2write.onNext(buffer);
                LOG.info("EchoProcessor #"+connSerialNum+" replied");
            } else {
                try {
                    serverConn.close();
                    complete();
                    LOG.info("EchoProcessor #"+connSerialNum+"completed");
                } catch (IOException e) {
                    completeExceptionally(e);
                    LOG.info("EchoProcessor #"+connSerialNum+"completed with error "+e);
                }
            }
        }
    }
}
