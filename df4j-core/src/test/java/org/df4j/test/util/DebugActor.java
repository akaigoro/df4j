package org.df4j.test.util;

import org.df4j.core.node.Actor;
import org.df4j.core.util.SameThreadExecutor;

public abstract class DebugActor extends Actor {
    {
        super.setExecutor(new SameThreadExecutor());
        start();
    }
}
