package org.df4j.core.util.asyncmon;

public interface AsyncMonitor {
    void doWait();
    void doNotify();
    void doNotifyAll();
}
