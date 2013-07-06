package com.github.rfqu.df4j.nio.echo;

import java.nio.ByteBuffer;

import com.github.rfqu.df4j.nio.AsyncSocketChannel;
import com.github.rfqu.df4j.nio.IOHandler;
import com.github.rfqu.df4j.nio.SocketIORequest;

class ServerConnection {
    private final EchoServer echoServer;
    private AsyncSocketChannel channel;
    public int id;
    private ByteBuffer buffer;
    private SerRequest request;

    public ServerConnection(EchoServer echoServer, AsyncSocketChannel channel)
    //        throws ClosedChannelException
    {
        this.echoServer = echoServer;
        this.channel=channel;
        this.id=echoServer.ids.addAndGet(1);
        buffer = ByteBuffer.allocate(EchoServer.BUF_SIZE);
        request = new SerRequest(buffer);
        channel.read(request);
        request.addListener(endRead);
    }

    public synchronized void close() {
        if (channel.isClosed()) {
            return;
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
            // imitate reading client's message and writing it back
            buffer.position(buffer.limit());
            // write it to socket
            channel.write(request);
            request.addListener(endWrite);
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
            channel.read(request);
            request.addListener(endRead);
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
