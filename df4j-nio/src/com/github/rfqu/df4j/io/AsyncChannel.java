package com.github.rfqu.df4j.io;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;

import com.github.rfqu.df4j.core.Link;

public abstract class AsyncChannel extends Link {
    AsyncSelector selector;

    protected AsyncChannel() throws IOException {
        selector =AsyncSelector.getCurrentSelector();
    }
    
    private int interest=0; // current registered interest
    
    protected  void interestOn(int bit) throws ClosedChannelException {
        synchronized (this) {
            if ((interest & bit) != 0) {
                return; // set already
            }
            interest = interest | bit;
        }
        selector.register(this, interest);        
    }

    protected  void interestOff(int bit) throws ClosedChannelException {
        synchronized (this) {
            if ((interest & bit) == 0) {
                return; // not set
            }
            interest = interest & ~bit;
        }
        selector.register(this, interest);        
    }

    public void close() throws IOException {
        SelectableChannel channel = getChannel();
        if (channel!=null) {
            channel.close();
        }
    }

    public abstract SelectableChannel getChannel();

    public abstract void notify(SelectionKey key);
    
}
