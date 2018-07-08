package org.df4j.core.node;

import java.lang.reflect.InvocationTargetException;

public class Actor extends AsyncAction {

    @Override
    protected void runAction() throws IllegalAccessException, InvocationTargetException {
        super.runAction();
        start();
    }

}
