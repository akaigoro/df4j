package org.df4j.tricky.charflow.jsontokens;

import java.util.Objects;

public class NumberToken extends Token {
    public final Integer value;

    public NumberToken(Integer value) {
        super(TokenType.Number);
        this.value = value;
    }

    public static NumberToken of(Integer value) {
        return new NumberToken(value);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof NumberToken)) return false;
        if (!super.equals(o)) return false;
        NumberToken that = (NumberToken) o;
        return value.equals(that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return "Value="+value;
    }
}
