package com.github.rfqu.df4j.nio;

import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;

public abstract class AbstractSelectorListener implements Runnable {
        SelectorThread selectorThread;
        SelectionKey key;
        /** set of SelectionKeys: OP_ACCEPT, OP_CONNECT, OP_READ, OP_WRITE */
        int interestOps=0;
        private int interestBits2on=0;
        private int interestBits2off=0;
        boolean fired=false;
        
        AbstractSelectorListener(SelectorThread selectorThread) throws ClosedChannelException {
            this.selectorThread=selectorThread;
        }

        /** 
         * Raises bit in the key's interest operations.
         * When selector return this key, the semaphore will be notified.
         * Note that if this channel has two bits raised simultaneously,
         * and one of them fired, then both semafores will be notified, 
         * one of them receiving false notification.
         * @param bit one of SelectorKey constants
         * @throws ClosedChannelException 
         */
        synchronized void interestOn(int bit) throws ClosedChannelException {
            if ((interestOps & bit)!=0) { // bit set already
              return;
            }
            if ((interestBits2on & bit)!=0) { // bit set already
              return;
            }
            interestBits2on |= bit;
            if (fired) {
                return;
            }
            fired=true;
            selectorThread.execute(this);
        }
        
        synchronized void interestOff(int bit) throws ClosedChannelException {
            if ((interestOps & bit)==0) { // bit not set yet
                return;
            }
            if ((interestBits2off & bit)!=0) { // bit unset already
                return;
            }
            interestBits2off |= bit;
            if (fired) {
                return;
            }
            fired=true;
            selectorThread.execute(this);
        }
        
        // to change interest bits
        public synchronized void run(SelectableChannel selectableChannel) throws ClosedChannelException {
            int newInterestOps=interestOps & ~interestBits2off | interestBits2on;
            if (newInterestOps==interestOps) {
                return;
            }
            interestOps=newInterestOps;
            if (key==null) {
                key=selectableChannel.register(selectorThread.selector, interestBits2on, this);
            } else {
                key.interestOps(interestOps);
            }
        }
        
        // react to key events
        abstract void run(SelectionKey key);
    }