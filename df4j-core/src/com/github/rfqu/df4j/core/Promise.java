package com.github.rfqu.df4j.core;

public interface Promise<R> {

    public <S extends Port<R>> S request(S sink);
    
}
