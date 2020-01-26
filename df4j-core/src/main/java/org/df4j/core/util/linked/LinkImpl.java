package org.df4j.core.util.linked;

public abstract class LinkImpl<T> implements Link<T> {
    private Link<T> prev = this;
    private Link<T> next = this;

    @Override
    public Link<T> getNext() {
        return next;
    }

    @Override
    public void setNext(Link<T> next) {
        this.next = next;
    }

    @Override
    public Link<T> getPrev() {
        return prev;
    }

    @Override
    public void setPrev(Link<T> prev) {
        this.prev = prev;
    }
}
