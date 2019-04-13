package org.df4j.core.util.linked;

public abstract class Link<S extends Link<S>> {
    protected Link<S> prev;

    public boolean isLinked() {
        return prev != null;
    }
}
