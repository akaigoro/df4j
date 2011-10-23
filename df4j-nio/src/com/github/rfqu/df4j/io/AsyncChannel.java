/*
 * Copyright 2011 by Alexei Kaigorodov
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package com.github.rfqu.df4j.io;

import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;

import com.github.rfqu.df4j.core.Link;

public abstract class AsyncChannel extends Link {
    private int interest=0; // current registered interest
    private boolean registering=false; // current registered interest
    private AsyncSelector selector;

    public AsyncChannel(AsyncSelector selector) {
        this.selector = selector;
    }

    
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
        selector.notify(this);        
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
        selector.notify(this);        
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
