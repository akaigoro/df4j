package org.df4j.nio2.net.echo;

import org.df4j.nio2.net.ServerConnection;

import java.util.function.Consumer;

public class EchoServer extends ServerConnection {

    public EchoServer(Consumer<ServerConnection> backport) {
        super("EchoServerConnection", backport);
        // returns each received ByteBuffer to the client
        reader.output.subscribe(writer.input);
        // directs all used buffers after writing to the reader
        writer.output.subscribe(reader.input);
        Utils.injectBuffers(2, 128, reader);
        LOG.config(getClass().getName()+" created");
    }
}
