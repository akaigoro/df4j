package com.github.rfqu.df4j.nio1;

import java.nio.channels.SelectionKey;

public interface SelectorEventListener {
    void onSelectorEvent(SelectionKey key);
    /** invoked by selector service when attemping to register on closed channel */
//    void close();
}
