package org.df4j.reflect;

import org.df4j.core.node.Action;
import org.df4j.core.util.ActionCaller;
import org.junit.Assert;
import org.junit.Test;

import java.util.function.Function;

public class ActionCallerTest {
    static final Integer ONE = Integer.valueOf(1);
    static final Integer TWO = Integer.valueOf(2);
    static final Integer THREE = Integer.valueOf(3);
    static final Integer FOUR = Integer.valueOf(4);

    @Test(expected = NoSuchMethodException.class)
    public void emptyClass_0() throws NoSuchMethodException {
        new ActionCaller<>(new Empty(), 0);
    }

    @Test(expected = NoSuchMethodException.class)
    public void emptyClass_1() throws NoSuchMethodException {
        new ActionCaller<>(new Empty(), 1);
    }

    @Test(expected = NoSuchMethodException.class)
    public void manyFields() throws NoSuchMethodException {
        new ActionCaller<>(new WithField_2(), 1);
    }

    @Test(expected = NoSuchMethodException.class)
    public void nullField() throws NoSuchMethodException {
        new ActionCaller<>(new WithField_1(null), 1);
    }

    @Test
    public void notNullField() throws Exception {
        Function<Integer, Integer> f = (v)->v*2;
        ActionCaller<Object> ac = new ActionCaller<>(new WithField_1(f), 1);
        Integer res1 = (Integer) ac.apply(ONE);
        Assert.assertEquals(TWO, res1);
        Integer res2 = (Integer) ac.apply(TWO);
        Assert.assertEquals(FOUR, res2);
    }

    @Test
    public void nullFieldAndMethod() throws Exception {
        ActionCaller<Object> ac = new ActionCaller<>(new WithFieldAndMethod(null), 1);
        Integer res1 = (Integer) ac.apply(TWO);
        Assert.assertEquals(ONE, res1);
        res1 = (Integer) ac.apply(FOUR);
        Assert.assertEquals(THREE, res1);
    }

    @Test
    public void notNullFieldAndMethod() throws Exception {
        Function<Integer, Integer> f = (v)->v*2;
        ActionCaller<Object> ac = new ActionCaller<>(new WithFieldAndMethod(f), 1);
        Integer res1 = (Integer) ac.apply(ONE);
        Assert.assertEquals(TWO, res1);
        Integer res2 = (Integer) ac.apply(TWO);
        Assert.assertEquals(FOUR, res2);
    }

    static class Empty {}

    static class WithField_1 {
        @Action
        final Function function;

        WithField_1(Function function) {
            this.function = function;
        }
    }

    static class WithField_2 {
        @Action
        Function function1;
        @Action
        Function function2;
    }

    static class WithFieldAndMethod {
        @Action
        final Function function;

        WithFieldAndMethod(Function function) {
            this.function = function;
        }

        @Action
        public int dec(int arg) {return arg-1;}
    }
}
