package org.df4j.core.util;

import java.util.logging.ConsoleHandler;
import java.util.logging.Level;

public class Logger extends java.util.logging.Logger {

    protected Logger(String name) {
        super(name, null);
    }

    public static Logger getLogger(String name) {
        Logger logger = new Logger(name);
        logger.setLevel(Level.FINE);
        logger.addHandler(new ConsoleHandler());
        return logger;
    }
}
