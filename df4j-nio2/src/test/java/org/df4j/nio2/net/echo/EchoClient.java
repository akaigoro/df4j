package org.df4j.nio2.net.echo;

import org.df4j.core.dataflow.Actor;
import org.df4j.core.dataflow.AsyncProc;
import org.df4j.core.dataflow.Dataflow;
import org.df4j.core.port.InpFlow;
import org.df4j.core.port.OutFlow;
import org.df4j.core.util.Logger;
import org.df4j.nio2.net.AsyncSocketChannel;
import org.df4j.nio2.net.AsyncClientSocketChannel;
import org.junit.Assert;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.charset.Charset;
import java.util.logging.Level;

/**
 * sends and receives limited number of messages
 */
class EchoClient extends AsyncProc {
    static final Charset charset = Charset.forName("UTF-16");

    protected final Logger LOG = new Logger(this, Level.INFO);
    public final int total;
    public int count;
    protected AsyncClientSocketChannel inp = new AsyncClientSocketChannel(this);

    AsyncSocketChannel clientConn;
    Speaker speaker;
    Listener listener;

    public EchoClient(Dataflow dataflow, SocketAddress local9990, int total) throws IOException {
        super(dataflow);
        this.total = total;
        inp.connect(local9990);
    }

    public void runAction() {
        AsynchronousSocketChannel assc = inp.current();
        clientConn = new AsyncSocketChannel(getParent(), assc);
        clientConn.setName("client");
        speaker = new Speaker();
        speaker.buffers2write.subscribe(clientConn.writer.input);
        clientConn.writer.output.subscribe(clientConn.reader.input);
        listener = new Listener();
        speaker.sentMsgs.subscribe(listener.sentMsgs);
        clientConn.reader.output.subscribe(listener.readBuffers);
        speaker.start();
        listener.start();
        LOG.info("Speaker and listener started");
    }

    public static ByteBuffer toByteBuf(String message) {
        return ByteBuffer.wrap(message.getBytes(charset));
    }

    public static String fromByteBuf(ByteBuffer b) {
        return new String(b.array(), charset);
    }

    class Speaker extends Actor {
        private int count;
        OutFlow<ByteBuffer> buffers2write = new OutFlow<>(this);
        OutFlow<String> sentMsgs = new OutFlow<>(this, 10);

        public Speaker() {
            super(EchoClient.this.getParent());
            this.count = total;
        }

        public void runAction() {
            String message = "hi there "+count;
            ByteBuffer buf = toByteBuf(message);
            buffers2write.onNext(buf);
            if (count > 0) {
                sentMsgs.onNext(message);
                LOG.info("Speaker sent message: "+message);
            } else {
                sentMsgs.onComplete();
                LOG.info("Speaker finished successfully");
                onComplete();
            }
            count--;
        }
    }

    class Listener extends Actor {
        InpFlow<String> sentMsgs = new InpFlow<>(this);
        InpFlow<ByteBuffer> readBuffers = new InpFlow<>(this);

        public Listener() {
            super(EchoClient.this.getParent());
        }

        @Override
        public void onComplete() {
            try {
                clientConn.close();
                super.onComplete();
            } catch (IOException e) {
                super.onError(e);
            }
        }

        public void runAction() {
            if (sentMsgs.isCompleted() || readBuffers.isCompleted()) {
                this.onComplete();
                LOG.info("Listener finished successfully");
                return;
            }
            String sent = sentMsgs.remove();
            ByteBuffer received = readBuffers.remove();
            String m2 = fromByteBuf(received);
            LOG.info("Listener received message:"+m2);
            Assert.assertEquals(sent, m2);
        }

    }

}
