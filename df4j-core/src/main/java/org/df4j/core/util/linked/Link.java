package org.df4j.core.util.linked;

public interface Link {
    Link getNext();

    void setNext(Link next);

    Link getPrev();

    void setPrev(Link prev);

    default boolean isLinked() {
        return getNext() != this;
    }

    default void unlink() {
        getPrev().setNext(this.getNext());
        getNext().setPrev(this.getPrev());
        this.setPrev(this);
        this.setNext(this);
    }
}
