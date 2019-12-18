package org.df4j.nio2.net.echo;

import org.df4j.core.communicator.AsyncSemaphore;
import org.df4j.nio2.net.ServerConnection;

import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;

public class EchoServerConnection extends ServerConnection {

    public EchoServerConnection(AsynchronousSocketChannel socket, AsyncSemaphore allowedConnections) {
        super(socket, "EchoServerConnection", allowedConnections);
        // returns each received ByteBuffer to the client
        reader.output.subscribe(writer.input);
        // directs all used buffers after writing to the reader
        writer.output.subscribe(reader.input);
        for (int k = 0; k< 1; k++) {
            ByteBuffer buf=ByteBuffer.allocate(128);
            reader.input.onNext(buf);
        }
        String name = getClass().getCanonicalName();
  //      LOG.config(name +" created");
    }
}
