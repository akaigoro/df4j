package org.df4j.core.util.linked;

public abstract class Link<L extends Link<L>> {
    protected L prev;

    public boolean isLinked() {
        return prev != null;
    }
}
