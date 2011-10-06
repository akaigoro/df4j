package com.github.rfqu.df4j.io;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;

import com.github.rfqu.df4j.core.Link;

public abstract class AsyncChannel extends Link {
    AsyncSelector selector;

    protected AsyncChannel() throws IOException {
        selector = AsyncSelector.getCurrentSelector();
    }
    
    private int interest=0; // current registered interest
    private boolean registering=false; // current registered interest
    
    protected  void interestOn(int bit) throws ClosedChannelException {
        synchronized (this) {
            if ((interest & bit) != 0) {
                return; // set already
            }
            interest = interest | bit;
            if (registering) {
                return;
            }
            registering=true;
        }
        selector.startRegistration(this);        
    }

    protected  void interestOff(int bit) throws ClosedChannelException {
        synchronized (this) {
            if ((interest & bit) == 0) {
                return; // not set
            }
            interest = interest & ~bit;
            if (registering) {
                return;
            }
            registering=true;
        }
        selector.startRegistration(this);        
    }

    void doRegistration(Selector selector) throws ClosedChannelException {
        synchronized (this) {
            getChannel().register(selector, interest, this);
            registering=false;
        }
    }
    
    public abstract SelectableChannel getChannel();

    abstract void notify(SelectionKey key);

}
