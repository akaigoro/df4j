package com.github.rfqu.df4j.nio;

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
    /** set of SelectionKeys: OP_ACCEPT, OP_CONNECT, OP_READ, OP_WRITE */
    protected int interestBits;
    private boolean fired;
    
    public SelectorEventListener(Selector selector) {
        this.selector = selector;
    }

    protected void interestOn(int bit) {
        interestBits|=bit;
    }
    
    protected void interestOff(int bit) {
        interestBits&=~bit;
    }
    
    public void listen(int bit) {
        synchronized(this) {
            interestOn(bit);
            if (fired){
               return;
            }
            fired=true;
        }
    }
    
    abstract public void run(SelectionKey key);

    abstract protected void channelClosed();

}