package org.df4j.core.util.linked;

public interface Link<T> {
    Link<T> getNext();

    void setNext(Link<T> next);

    Link<T> getPrev();

    void setPrev(Link<T> prev);

    T getItem();

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
