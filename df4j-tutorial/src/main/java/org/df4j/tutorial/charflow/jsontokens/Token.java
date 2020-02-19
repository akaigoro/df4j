package org.df4j.tutorial.charflow.jsontokens;

import java.util.Objects;

public class Token {
    public final TokenType type;

    public Token(TokenType type) {
        this.type = type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Token token = (Token) o;
        return type == token.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(type);
    }

    @Override
    public String toString() {
        return type.toString();
    }

    public static Token of(TokenType type) {
        return new Token(type); // todo cache
    }
}
