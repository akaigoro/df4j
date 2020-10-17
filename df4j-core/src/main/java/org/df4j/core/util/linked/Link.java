package org.df4j.core.util.linked;

public interface Link {
    Link getNext();
    Link getPrev();

    void setNext(Link next);
    void setPrev(Link prev);

    /**
     * @return true if this item is linked to some {@link LinkedQueue}
     */
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
