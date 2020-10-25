package org.df4j.nio2.net.echo;

import ch.qos.logback.classic.Level;
import org.df4j.core.actor.Actor;
import org.df4j.core.actor.ActorGroup;
import org.df4j.core.port.InpFlood;
import org.df4j.core.util.LoggerFactory;
import org.df4j.nio2.net.AsyncServerSocketChannel;
import org.df4j.nio2.net.Connection;
import org.slf4j.Logger;

import java.io.IOException;
import java.net.SocketAddress;
import java.util.HashSet;
import java.util.Set;

/**
 * generates {@link EchoProcessor}s for incoming connections
 *
 */
public class EchoServer extends Actor {
    public static final int BUF_SIZE = 128;
    protected final Logger LOG = LoggerFactory.getLogger(this, Level.INFO);
    InpFlood<Connection> inp = new InpFlood<>(this);
    AsyncServerSocketChannel connProducer;
    Set<EchoProcessor> echoProcessors = new HashSet<>();

    public EchoServer(ActorGroup dataflow, SocketAddress addr, int maxConnCount) throws IOException {
        super(dataflow);
        connProducer = new AsyncServerSocketChannel(dataflow, addr);
        connProducer.out.subscribe(inp);
        connProducer.release(maxConnCount);
        connProducer.start();
    }

    public void whenComplete() {
        connProducer.complete();
        for (EchoProcessor processor: echoProcessors) {
            processor.complete();
        }
    }

    @Override
    protected void runAction() throws Throwable {
        if (inp.isCompletedExceptionally()) {
            completeExceptionally(inp.getCompletionException());
            return;
        } else if (inp.isCompleted()) {
            complete();
            return;
        }
        Connection conn = inp.remove();
        EchoProcessor processor = new EchoProcessor(this, getActorGroup(), conn);
        echoProcessors.add(processor); 
        processor.start();
    }

}
