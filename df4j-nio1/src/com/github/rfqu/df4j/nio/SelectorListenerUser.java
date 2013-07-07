package com.github.rfqu.df4j.nio;

import java.nio.channels.SelectableChannel;
import java.nio.channels.Selector;

public interface SelectorListenerUser {
    Selector getSelector();
    SelectableChannel getChannel();
    void close();
}
