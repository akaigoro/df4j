package org.df4j.test.util;

import org.df4j.core.Port;

/**
 * the type of messages floating between nodes
 */
public class Packet {
    int hops_remained;
    Port<Packet> sink;
    Port<Packet> sender;

    public Packet(int hops_remained, Port<Packet> sink) {
        this.hops_remained = hops_remained;
        this.sink = sink;
    }
    
    public void send(Port<Packet> from, Port<Packet> to) {
        if (--hops_remained > 0) {
            sender=from;
            to.post(this);
        } else {
            sink.post(this);
        }
    }

    public void reply() {
        if (--hops_remained > 0) {
            sender.post(this);
        } else {
            sink.post(this);
        }
    }
}