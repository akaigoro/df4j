package org.df4j.test.util;

/**
 * the type of messages floating between nodes
 */
public class Token {//extends Request<Token, Void> {
    public int hops_remained;

    public Token(int hops_remained) {
        this.hops_remained = hops_remained;
    }
}