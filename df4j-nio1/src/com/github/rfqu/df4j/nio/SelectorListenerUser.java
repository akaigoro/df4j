package com.github.rfqu.df4j.nio;

import java.nio.channels.SelectableChannel;

public interface SelectorListenerUser {
    SelectableChannel getChannel();
    void close();
}
