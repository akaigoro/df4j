package org.df4j.tck;

import org.reactivestreams.tck.TestEnvironment;
import org.testng.annotations.Test;

import java.lang.reflect.Field;

public abstract class PublisherVerification extends org.reactivestreams.tck.PublisherVerification<Long> {
    protected final TestEnvironment env;

    public PublisherVerification(TestEnvironment env) throws NoSuchFieldException, IllegalAccessException {
        super(env);
        Field fenv = org.reactivestreams.tck.PublisherVerification.class.getDeclaredField("env");
        fenv.setAccessible(true);
        this.env = (TestEnvironment) fenv.get(this);
    }


    public PublisherVerification(int timeout) throws NoSuchFieldException, IllegalAccessException {
        this(new TestEnvironment(timeout));
    }
}