package org.df4j.nio2.net.echo;

import org.df4j.core.actor.Actor;
import org.df4j.core.actor.ActorGroup;
import org.df4j.core.util.Logger;
import org.df4j.nio2.net.ServerSocketPort;
import org.df4j.nio2.net.Connection;

import java.io.IOException;
import java.net.SocketAddress;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;

/**
 * generates {@link EchoProcessor}s for incoming connections
 *
 */
public class EchoServer extends Actor {
    public static final int BUF_SIZE = 128;
    protected final Logger LOG = new Logger(this, Level.INFO);
    ServerSocketPort inp = new ServerSocketPort(this);
    Set<EchoProcessor> echoProcessors = new HashSet<>();
    long connBSerialNum = 0;

    public EchoServer(ActorGroup actorGroup, SocketAddress socketAddressdr) throws IOException {
        super(actorGroup);
        inp.connect(socketAddressdr, 2);
    }

    public void whenComplete() {
        LOG.info(" completion started");
        inp.cancel();
        for (EchoProcessor processor: echoProcessors) {
            processor.complete();
        }
    }

    public void whenError(Throwable e) {
        whenComplete();
    }

    @Override
    protected void runAction() throws Throwable {
        if (inp.isCompleted()) {
            complete();
            return;
        }
        Connection serverConn = inp.remove();
        LOG.info(" request accepted");
        EchoProcessor processor =
                new EchoProcessor(getDataflow(), serverConn, connBSerialNum++); // create client connection
        processor.start();
    }
}
