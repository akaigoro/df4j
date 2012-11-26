package com.github.rfqu.df4j.nio;

import java.nio.channels.SelectionKey;

public interface SocketEventListener {
    void accept(SelectionKey key);
    void connect(SelectionKey key);
    void read(SelectionKey key);
    void write(SelectionKey key);
    
    /** invoked by selector service when attemping to register on closed channel */
    void close();
}
