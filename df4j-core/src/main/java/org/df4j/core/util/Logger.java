package org.df4j.core.util;

import org.jetbrains.annotations.NotNull;

import java.util.logging.ConsoleHandler;
import java.util.logging.Level;

public class Logger extends java.util.logging.Logger {

    protected Logger(String name) {
        super(name, null);
    }

    public Logger(Object parent) {
        super(parent.getClass().getName(), null);
    }

    public static Logger getLogger(String name) {
        return getLogger(name, Level.FINE);
    }

    @NotNull
    public static Logger getLogger(String name, Level level) {
        Logger logger = new Logger(name);
        logger.setLevel(level);
        logger.addHandler(new ConsoleHandler());
        return logger;
    }
}
