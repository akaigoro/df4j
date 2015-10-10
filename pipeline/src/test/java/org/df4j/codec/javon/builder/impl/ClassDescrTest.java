package org.df4j.codec.javon.builder.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import org.df4j.pipeline.codec.javon.builder.impl.ClassDescr;
import org.junit.Assert;
import org.junit.Test;

public class ClassDescrTest {

    @Test
    public void testString() throws Exception {
        ClassDescr strDescr=new ClassDescr(String.class);
        Object actual=strDescr.newInstance();
        Assert.assertEquals("", actual);
        Object actual1=strDescr.newInstance("createTest1");
        Assert.assertEquals("createTest1", actual1);
    }

    @Test
    public void testData1() throws Exception {
        ClassDescr dataDescr=new ClassDescr(Data.class);
        checkData1(dataDescr, 0);
        checkData1(dataDescr, 1);
        checkData1(dataDescr, 2);
    }

    protected void checkData1(ClassDescr classDescr, Integer arg) throws Exception {
        Object expected=new Data(arg);
        Object actual=classDescr.newInstance(arg);
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testData2() throws Exception {
        ClassDescr dataDescr=new ClassDescr(Data.class);
        checkData2(dataDescr, 0, new ArrayList<Integer>());
        checkData2(dataDescr, 1, null);
        checkData2(dataDescr, 2, new Vector<String>());
    }

    protected void checkData2(ClassDescr classDescr, int arg1, List<?> arg2) throws Exception {
        Data expected=new Data(arg1, arg2);
        Data actual=(Data) classDescr.newInstance(arg1, arg2);
        Assert.assertEquals(expected, actual);
        Assert.assertEquals(expected.getValue(), classDescr.get(actual, "value"));
        Assert.assertTrue(expected.getList()==classDescr.get(actual, "list"));
    }

    @Test
    public void setTest1() throws Exception {
        ClassDescr dataDescr=new ClassDescr(Data.class);
        checkSet1(dataDescr, 0, new ArrayList<Integer>());
        checkSet1(dataDescr, 1, null);
        checkSet1(dataDescr, 2, new Vector<String>());
    }

    protected void checkSet1(ClassDescr classDescr, int arg1, List<?> arg2) throws Exception {
        Data expected=new Data(arg1);
        expected.setList(arg2);
        Object actual=classDescr.newInstance(arg1);
        classDescr.set(actual, "list", arg2);
        Assert.assertEquals(expected, actual);
    }

    static class Data {
        int value;
        List<?> list;

        public Data(int value) {
            this.value = value;
        }

        public Data(int value, List<?> list) {
            this.value = value;
            this.list = list;
        }

        public int getValue() {
            return value;
        }

        public void setValue(int value) {
            this.value = value;
        }

        public List<?> getList() {
            return list;
        }

        public void setList(List<?> list) {
            this.list = list;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            Data other = (Data) obj;
            if (list == null) {
                if (other.list != null)
                    return false;
            } else if (!list.equals(other.list))
                return false;
            if (value != other.value)
                return false;
            return true;
        }
        
    }
}

