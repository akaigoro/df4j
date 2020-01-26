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

    default void offer(Link<T> other) {
        if (other == this) {
            throw new IllegalArgumentException();
        }
        other.setNext(this);
        Link<T> prev = getPrev();
        other.setPrev(prev);
        prev.setNext(other);
        this.setPrev(other);
    }

    default Link<T> poll() {
        Link<T> res = getNext();
        if (res == this) {
            return null;
        }
        res.unlink();
        return res;
    }

    default void unlink() {
        getPrev().setNext(this.getNext());
        getNext().setPrev(this.getPrev());
        this.setPrev(this);
        this.setNext(this);
    }
}
