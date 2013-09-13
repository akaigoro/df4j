package com.github.rfqu.df4j.examples;

/**
 * the type of messages floating between nodes
 */
class Token {//extends Request<Token, Void> {
    int hops_remained;

    public Token(int hops_remained) {
        this.hops_remained = hops_remained;
    }
}