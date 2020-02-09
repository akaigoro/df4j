package org.df4j.nio2.net.echo;

import org.df4j.core.dataflow.Actor;
import org.df4j.core.dataflow.AsyncProc;
import org.df4j.core.dataflow.Dataflow;
import org.df4j.core.port.InpFlow;
import org.df4j.core.port.InpScalar;
import org.df4j.core.port.InpSignal;
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
import java.util.logging.Level;

/**
 * generates {@link EchoProcessor}s for incoming connections
 *
 */
public class EchoServer extends Actor {
    protected final Logger LOG = new Logger(this, Level.INFO);
    /** limits the munber of simultaneously existing connections */
    protected InpSignal allowedConnections = new InpSignal(this);
    Set<EchoProcessor> echoProcessors = new HashSet<>();
    AsyncServerSocketChannel acceptor;

    public EchoServer(Dataflow dataflow, SocketAddress addr, int maxConnCount) throws IOException {
        super(dataflow);
        acceptor = new AsyncServerSocketChannel(dataflow, addr);
        acceptor.start();
        allowedConnections.release(maxConnCount);
    }

    public void close() throws InterruptedException {
        acceptor.close();
        onComplete();
        join();
        for (EchoProcessor processor: echoProcessors) {
            processor.onComplete();
        }
        for (EchoProcessor processor: echoProcessors) {
            processor.join();
        }
    }

    @Override
    public void runAction() {
        allowedConnections.acquire();
        Starter starter = new Starter(getParent());
        acceptor.demands.subscribe(starter.inp);
        starter.start();
    }

    class Starter extends AsyncProc {
        protected InpScalar<AsynchronousSocketChannel> inp = new InpScalar<>(this);

        public Starter(Dataflow dataflow) {
            super(dataflow);
            setDaemon(true);
        }

        @Override
        protected void runAction() {
            AsynchronousSocketChannel assc = inp.remove();
            EchoProcessor processor = new EchoProcessor(assc);
            echoProcessors.add(processor);
            processor.start();
        }
    }

    class EchoProcessor extends Actor {
        Starter starter;
        AsyncSocketChannel serverConn;
        InpFlow<ByteBuffer> readBuffers = new InpFlow<>(this);
        OutFlow<ByteBuffer> buffers2write = new OutFlow<>(this);
        private boolean connectionPermitReleased;

        public EchoProcessor(AsynchronousSocketChannel assc) {
            super(EchoServer.this.getParent());
            int capacity = 2;
            serverConn = new AsyncSocketChannel(getParent(), assc);
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

        public synchronized void releaseConnectionPermit() {
            if (connectionPermitReleased) {
                return;
            }
            connectionPermitReleased = true;
            allowedConnections.release(1);
        }

        @Override
        public void onComplete() {
            super.onComplete();
            releaseConnectionPermit();
        }

        @Override
        protected void onError(Throwable ex) {
            super.onError(ex);
            releaseConnectionPermit();
        }

        public void runAction() {
            if (!readBuffers.isCompleted()) {
                ByteBuffer b = readBuffers.removeAndRequest();
                buffers2write.onNext(b);
                LOG.info("EchoProcessor replied");
            } else {
                serverConn.close();
                onComplete();
                LOG.info("EchoProcessor completed");
            }
        }
    }
}
