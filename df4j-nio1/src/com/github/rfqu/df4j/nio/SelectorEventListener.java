/*
 * Copyright 2013 by Alexei Kaigorodov
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions
 * and limitations under the License.
 */
package com.github.rfqu.df4j.nio;

import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;

/** Lifecycle:
 *  setSelector(Selector);
 *  loop: {
 *    run();
 *    fixInterest();
 *  }
 * @author Alexei Kaigorodov
 *
 */
public abstract class SelectorEventListener {
    protected Selector selector;
    SelectableChannel channel;
    SelectionKey key;
    /** set of SelectionKeys: OP_ACCEPT, OP_CONNECT, OP_READ, OP_WRITE */
    protected int interestBits=0;
    
    public SelectorEventListener(Selector selector, SelectableChannel channel) throws ClosedChannelException {
        if (selector==null) {
            throw new NullPointerException();
        }
        if (channel==null) {
            throw new NullPointerException();
        }
        this.selector = selector;
        this.channel = channel;
    }

    /** 
     * @param bit
     * @throws ClosedChannelException 
     */
    protected void interestOn(int bit) throws ClosedChannelException {
        if ((interestBits&bit)!=0) { // bit set already
            return;
        }
        interestBits|=bit;
        if (key==null) {
            key=channel.register(selector, interestBits, this);
        } else {
            key.interestOps(interestBits);
        }
    }
    
    protected void interestOff(int bit) throws ClosedChannelException {
        if ((interestBits&bit)==0) {  // bit unset already
            return;
        }
        interestBits&=~bit;
        if (key==null) {
            key=channel.register(selector, interestBits, this);
        } else {
            key.interestOps(interestBits);
        }
    }
    
    abstract public void run();

}