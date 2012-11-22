package com.github.rfqu.df4j.testutil;

import com.github.rfqu.df4j.core.Link;

/**
 * the type of messages floating between nodes
 */
public class IntValue extends Link {
    public int value;

    public IntValue(int value) {
        this.value = value;
    }
}