package com.github.rfqu.df4j.ioexample.dock;

import com.github.rfqu.df4j.core.Link;

public abstract class Action extends Link {
	abstract void act(Page p);
}