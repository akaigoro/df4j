package com.github.rfqu.df4j.io;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

import com.github.rfqu.df4j.core.MessageQueue;
import com.github.rfqu.df4j.core.Task;

public class AsyncServerSocketChannel extends AsyncChannel {
    ServerSocketChannel serverChannel;
    MessageQueue<AsyncSocketChannel> acceptors=new MessageQueue<AsyncSocketChannel>();

    public AsyncServerSocketChannel(InetSocketAddress listenAddr) throws IOException {
        serverChannel = ServerSocketChannel.open();
        serverChannel.socket().bind(listenAddr);
        serverChannel.configureBlocking(false);
        interestOn(SelectionKey.OP_ACCEPT);
    }

    @Override
    void notify(SelectionKey key) {
        try {
            for (;;) {
                synchronized (this) {
                    if (acceptors.isEmpty()) {
                        interestOff(SelectionKey.OP_ACCEPT);
                        return;
                    }
                }
                SocketChannel channel = serverChannel.accept();
                if (channel==null) {
                    return;
                }
                AsyncSocketChannel acceptor;
                synchronized (this) {
                    acceptor = acceptors.poll();
                }
                channel.configureBlocking(false);
                acceptor.connCompleted(channel);            
            }
        } catch (ClosedChannelException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /** offer from new AsyncServerSocket
     * 
     * @param acceptor
     * @return
     * @throws IOException
     * @throws ClosedChannelException
     */
    public void accept(AsyncSocketChannel acceptor) throws IOException, ClosedChannelException {
        SocketChannel res = serverChannel.accept();
        synchronized (this) {
            if (res == null) {
                acceptors.enqueue(acceptor);
                interestOn(SelectionKey.OP_ACCEPT);
                serverChannel.register(selector.selector, SelectionKey.OP_ACCEPT, this);
                return;
            }
        }
        res.configureBlocking(false);
        acceptor.connCompleted(res);
    }

    @Override
    public ServerSocketChannel getChannel() {
        return serverChannel;
    }

    public void close() throws IOException {
        serverChannel.close();
    }
    
}
