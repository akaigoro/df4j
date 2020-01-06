package org.df4j.core.util;

import org.jetbrains.annotations.NotNull;

import java.util.logging.ConsoleHandler;
import java.util.logging.Level;

public class Logger extends java.util.logging.Logger {

    protected Logger(String name) {
        super(name, null);
    }

    public Logger(Object parentObject) {
        super(parentObject.getClass().getName(), null);
        setLevel(Level.OFF);
        addHandler(new ConsoleHandler());
    }

    public Logger(Object parentObject, Level level) {
        this(parentObject);
        setLevel(level);
    }

    public static Logger getLogger(String name) {
        return getLogger(name, Level.FINE);
    }

    @NotNull
    public static Logger getLogger(String name, Level level) {
        Logger logger = new Logger(name);
        logger.setLevel(level);
        return logger;
    }
}
