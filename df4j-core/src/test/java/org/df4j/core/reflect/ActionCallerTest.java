package org.df4j.core.reflect;

import org.df4j.core.asynchproc.ext.Action;
import org.df4j.core.util.ActionCaller;
import org.df4j.core.util.invoker.FunctionInvoker;
import org.df4j.core.util.invoker.Invoker;
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
        ActionCaller.findAction(new Empty(), 0);
    }

    @Test(expected = NoSuchMethodException.class)
    public void emptyClass_1() throws NoSuchMethodException {
        ActionCaller.findAction(new Empty(), 1);
    }

    @Test(expected = NoSuchMethodException.class)
    public void manyFields() throws NoSuchMethodException {
        ActionCaller.findAction(new WithField_2(), 1);
    }

    @Test(expected = NoSuchMethodException.class)
    public void nullField() throws NoSuchMethodException {
        ActionCaller.findAction(new WithField_1(), 1);
    }

    @Test
    public void notNullField() throws Exception {
        Function<Integer, Integer> f = (v)->v*2;
        Invoker ac = ActionCaller.findAction(new WithField_1(f), 1);
        Assert.assertTrue(ac.returnsValue());
        Integer res1 = (Integer) ac.apply(ONE);
        Assert.assertEquals(TWO, res1);
        Integer res2 = (Integer) ac.apply(TWO);
        Assert.assertEquals(FOUR, res2);
    }

    @Test
    public void fieldWithAlienMethod() throws Exception {
        WithFieldAndMethod wfm = new WithFieldAndMethod();
        Function<Integer, Integer> f = wfm::dec;
        Invoker ac = ActionCaller.findAction(new WithField_1(f), 1);
        Assert.assertTrue(ac.returnsValue());
        Integer res1 = (Integer) ac.apply(TWO);
        Assert.assertEquals(ONE, res1);
        res1 = (Integer) ac.apply(FOUR);
        Assert.assertEquals(THREE, res1);
    }

    @Test
    public void fieldWithStaticMethod() throws Exception {
        Function<Integer, Integer> f = WithFieldAndMethod::inc;
        Invoker ac = ActionCaller.findAction(new WithField_1(f), 1);
        Assert.assertTrue(ac.returnsValue());
        Integer res1 = (Integer) ac.apply(ONE);
        Assert.assertEquals(TWO, res1);
        Integer res2 = (Integer) ac.apply(TWO);
        Assert.assertEquals(THREE, res2);
    }

    @Test
    public void nullFieldAndMethod() throws Exception {
        Invoker ac = ActionCaller.findAction(new WithFieldAndMethod(), 1);
        Assert.assertTrue(ac.returnsValue());
        Integer res1 = (Integer) ac.apply(TWO);
        Assert.assertEquals(ONE, res1);
        res1 = (Integer) ac.apply(FOUR);
        Assert.assertEquals(THREE, res1);
    }

    @Test
    public void notNullFieldAndMethod() throws Exception {
        Function<Integer, Integer> f = (v)->v*2;
        Invoker ac = ActionCaller.findAction(new WithFieldAndMethod(f), 1);
        Integer res1 = (Integer) ac.apply(ONE);
        Assert.assertEquals(TWO, res1);
        Integer res2 = (Integer) ac.apply(TWO);
        Assert.assertEquals(FOUR, res2);
    }

    @Test
    public void methodOnly() throws Exception {
        Invoker p = ActionCaller.findAction(new WithProc(), 0);
        Assert.assertFalse(p.returnsValue());
        Object res = p.apply();
        Assert.assertNull(res);
        Invoker f = ActionCaller.findAction(new WithFunc(), 0);
        Assert.assertTrue(f.returnsValue());
        Object res2 = f.apply();
        Assert.assertEquals(137, res2);
    }


    static class Empty {}

    static class WithField_1 {
        @Action
        final FunctionInvoker invoker;

        WithField_1() {
            this.invoker = null;
        }

        WithField_1(Function function) {
            this.invoker = new FunctionInvoker(function);
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
        final FunctionInvoker invoker;

        WithFieldAndMethod(Function function) {
            this.invoker = new FunctionInvoker(function);
        }

        WithFieldAndMethod() {
            this.invoker = null;
        }

        @Action
        public int dec(int arg) {return arg-1;}

        public static int inc(int arg) {return arg+1;}
    }

    static class WithProc {
        @Action
        public void proc() {

        }
    }

    static class WithFunc {
        @Action
        public int func() {
            return 137;
        }
    }
}
