package org.df4j.core.util;

import ch.qos.logback.classic.Level;
import org.slf4j.Logger;

public class LoggerFactory {

    public static Logger getLogger(String name, Level level) {
        Logger logger = org.slf4j.LoggerFactory.getLogger(name);
        setLevel(logger, level);
        return logger;
    }

    public static Logger getLogger(String name) {
        return getLogger(name, Level.INFO);
    }

    private static void setLevel(Logger logger, Level level) {
        ((ch.qos.logback.classic.Logger)logger).setLevel(level);
    }

    public static Logger getLogger(Object object, Level level) {
        return getLogger(object.getClass().getName(), level);
    }

    public static Logger getLogger(Object object) {
        return getLogger(object, Level.INFO);
    }
}
