package org.df4j.core.util;


import ch.qos.logback.classic.Level;
import org.slf4j.Logger;

public class LoggerFactory {

    public static Logger getLogger(String name) {
        return getLogger(name, Level.INFO);
    }

    public static Logger getLogger(String name, Level level) {
        Logger logger = org.slf4j.LoggerFactory.getLogger(name);
        ((ch.qos.logback.classic.Logger)logger).setLevel(level);
        return logger;
    }
    public static Logger getLogger(Object object, Level level) {
        return getLogger(object.getClass().getName(), level);
    }
    public static Logger getLogger(Object object) {
        return getLogger(object, Level.INFO);
    }
}
