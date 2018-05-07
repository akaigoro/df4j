package org.df4j.test.util;

import org.df4j.core.node.Actor;
import org.df4j.core.node.Actor1;
import org.df4j.core.util.SameThreadExecutor;

public abstract class DebugActor1<M> extends Actor1<M> {
    {
        super.setExecutor(new SameThreadExecutor());
        start();
    }
}
