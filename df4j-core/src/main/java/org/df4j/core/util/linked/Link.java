package org.df4j.core.util.linked;

public class Link<L extends Link<L>> {
    protected L next = (L) this;
    protected L prev = (L) this;

    public boolean isLinked() {
        return next != this;
    }

    public void offer(L other) {
        if (other == this) {
            throw new IllegalArgumentException();
        }
        other.next = (L) this;
        other.prev = prev;
        this.prev.next = other;
        this.prev = other;
    }

    public L poll() {
        L res = next;
        if (res == this) {
            return null;
        }
        res.unlink();
        return res;
    }

    protected void unlink() {
        prev.next = this.next;
        next.prev = this.prev;
        this.next = this.prev = (L) this;
    }

    public L getNext() {
        return next;
    }
}
