package org.df4j.core.util;

import java.util.Iterator;
import java.util.LinkedHashSet;

public class LinkedQueue<S> extends LinkedHashSet<S> {

    @Override
    public synchronized boolean add(S s) {
        return super.add(s);
    }

    public synchronized S remove() {
        Iterator<S> it = iterator();
        it.hasNext();
        return it.next();
    }

    public S poll() {
        Iterator<S> it = iterator();
        if (it.hasNext()) {
            S res = it.next();
            it.remove();
            return res;
        } else {
            return null;
        }
    }
}
