package org.df4j.core.util.linked;

public class LinkImpl implements Link {
    private Link prev = this;
    private Link next = this;

    @Override
    public Link getNext() {
        return next;
    }

    @Override
    public void setNext(Link next) {
        this.next = next;
    }

    @Override
    public Link getPrev() {
        return prev;
    }

    @Override
    public void setPrev(Link prev) {
        this.prev = prev;
    }
}
