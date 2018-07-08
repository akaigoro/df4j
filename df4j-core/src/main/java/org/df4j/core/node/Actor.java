package org.df4j.core.node;

import java.lang.reflect.InvocationTargetException;

/**
 * Actor is a reusable AsyncTask: after execution, it executes again as soon as new array of arguments is ready.
 */
public class Actor extends AsyncActionTask {

    @Override
    protected void runAction() throws IllegalAccessException, InvocationTargetException {
        super.runAction();
        start();
    }
}
