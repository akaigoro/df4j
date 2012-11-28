package com.github.rfqu.df4j.nio2.echo;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import com.github.rfqu.df4j.nio.AsyncSocketChannel;
import com.github.rfqu.df4j.nio.SocketIORequest;

class ServerConnection {
    private final EchoServer echoServer;
    AsyncSocketChannel channel;
    public int id;
    private ByteBuffer buffer;
    SerRequest request;
    boolean closed = false;

    public ServerConnection(EchoServer echoServer, AsyncSocketChannel channel)
    //        throws ClosedChannelException
    {
        this.echoServer = echoServer;
        this.channel=channel;
        this.id=echoServer.ids.addAndGet(1);
        buffer = ByteBuffer.allocate(EchoServer.BUF_SIZE);
        request = new SerRequest(buffer);
        channel.read(request, endRead);
    }

    public void close() {
        synchronized (this) {
            if (closed) {
                return;
            }
            closed = true;
        }
        try {
			channel.close();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			echoServer.connClosed(this);
		}
    }

    IOHandler<SerRequest> endRead = new IOHandler<SerRequest>() {
        @Override
		public void completed(int result, SerRequest request) {
            // System.out.println("  ServerRequest readCompleted id="+id);
            // read client's message as if all the data have been read
            buffer.position(buffer.limit());
            // write it back
            channel.write(request, endWrite);
        }

        @Override
		public void closed(SerRequest request) {//throws IOException {
            ServerConnection.this.close();
        }
    };

    IOHandler<SerRequest> endWrite = new IOHandler<SerRequest>() {
        @Override
		public void completed(int result, SerRequest request) {//throws IOException {
            // System.out.println("  ServerRequest writeCompleted id="+id);
            channel.read(request, endRead);
        }

        @Override
		public void closed(SerRequest request) {//throws IOException {
            ServerConnection.this.close();
        }
    };

    static class SerRequest extends SocketIORequest<SerRequest> {

        public SerRequest(ByteBuffer buf) {
            super(buf);
        }
    }
}
