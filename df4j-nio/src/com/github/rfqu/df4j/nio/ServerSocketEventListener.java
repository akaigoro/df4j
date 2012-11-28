package com.github.rfqu.df4j.nio;

import java.nio.channels.SelectionKey;

public interface ServerSocketEventListener {
    void accept(SelectionKey key);
    /** invoked by selector service when attemping to register on closed channel */
    void close();
}
