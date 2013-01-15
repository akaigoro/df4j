package com.github.rfqu.df4j.examples;

import com.github.rfqu.df4j.core.Link;
import com.github.rfqu.df4j.core.Port;

/**
 * the type of messages floating between nodes
 */
class Packet extends Link {
    int hops_remained;
    Port<Packet> sink;
    Port<Packet> sender;

    Packet(int hops_remained, Port<Packet> sink) {
        this.hops_remained = hops_remained;
        this.sink = sink;
    }
    
    void send(Port<Packet> from, Port<Packet> to) {
        if (--hops_remained > 0) {
            sender=from;
            to.post(this);
        } else {
            sink.post(this);
        }
    }

    void reply() {
        if (--hops_remained > 0) {
            sender.post(this);
        } else {
            sink.post(this);
        }
    }
}