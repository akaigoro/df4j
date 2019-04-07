package org.df4j.core.actor;

import org.df4j.core.asyncproc.AllOf;

import java.io.PrintStream;

public class Logger extends AllOf {
    PrintStream out = System.out;
    final boolean printOn;

    public Logger(boolean printOn) {
        this.printOn = printOn;
    }

    void println(String s) {
        if (!printOn) {
            return;
        }
        out.println(s);
        out.flush();
    }
}
