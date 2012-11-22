package com.github.rfqu.df4j.testutil;

import com.github.rfqu.df4j.core.Link;

public class StringMessage extends Link {
    private String str;

    public StringMessage(String str) {
        this.setStr(str);
    }

    @Override
    public String toString() {
        return getStr();
    }

    public String getStr() {
        return str;
    }

    public void setStr(String str) {
        this.str = str;
    }
}