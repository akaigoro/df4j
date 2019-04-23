package org.df4j.core.asyncproc;

public class ScalarParam<T> extends ScalarLock {
    protected T current;

    public ScalarParam(Transition transition) {
        super(transition);
    }

    @Override
    public boolean isParameter() {
        return true;
    }

    public T current() {
        return getCurrent();
    }

    public boolean moveNext() {
        throw new UnsupportedOperationException();
    }

    public T getCurrent() {
        return current;
    }

    public void setCurrent(T current) {
        this.current = current;
    }
}
