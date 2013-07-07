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

import java.nio.channels.CancelledKeyException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;

import com.github.rfqu.df4j.core.DataflowVariable.Semafor;

/** Lifecycle:
 *  setSelector(Selector);
 *  loop: {
 *    run();
 *    fixInterest();
 *  }
 * @author Alexei Kaigorodov
 *
 */
public class SelectorListener {
    private SelectorListenerUser asyncChannel;
    SelectionKey key;
    /** set of SelectionKeys: OP_ACCEPT, OP_CONNECT, OP_READ, OP_WRITE */
    private int interestBits=0;
    private Semafor[] semafores=new Semafor[5];
    
    SelectorListener(SelectorListenerUser asyncChannel) throws ClosedChannelException {
        this.asyncChannel=asyncChannel;
    }

    /** 
     * @param bit
     * @throws ClosedChannelException 
     */
    void interestOn(int bit, Semafor sema) throws ClosedChannelException {
        if (sema==null) {
            throw new IllegalArgumentException("sema=null");
        }
        int bitPos=bitPosByBit[bit];
        semafores[bitPos]=sema;
        if ((interestBits&bit)!=0) { // bit set already
            System.err.println("interest On: "+bitPosToString[bitPos]+ " already");
            return;
        }
        System.err.println("interest On: "+bitPosToString[bitPos]);
        interestBits|=bit;
        if (key==null) {
            key=asyncChannel.getChannel().register(asyncChannel.getSelector(), interestBits, this);
        } else {
            key.interestOps(interestBits);
        }
    }
    
    public void run() {
        int interestOps=key.interestOps();
        System.err.println("listener started: "+asyncChannel+"; bits="+interestOps);
        try {
            for (int bitPos=0; bitPos<5; bitPos++) {
                int bit=1<<bitPos;
                if ((interestOps&bit)==0) continue;
                if (semafores[bitPos]==null) {
                    System.err.println("semafores["+bitPos+"]==null bitK="+bitPos);
                }
                semafores[bitPos].up();
                semafores[bitPos]=null;
                if ((interestBits&bit)==0) {  // bit unset already
                    System.err.println("interest Off: "+bitPosToString[bitPos]+ " already");
                    continue;
                }
                System.err.println("interest Off: "+bitPosToString[bitPos]);
            }
        } catch (CancelledKeyException e) {
             asyncChannel.close();
        } finally {
             interestBits=0;
             key.interestOps(0);
        }
    }

    private static final int[] bitPosByBit = {0,0,1,0,2,0,0,0,3,0,0,0,0,0,0,0,4};
    private static final String[] bitPosToString={"READ","UNISED","WRITE","ACCEPT","CONNECT"};
}