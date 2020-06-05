package org.df4j.tricky.charflow.jsontokens;

import java.util.Objects;

public class StringToken extends Token {
    public final String value;

    public StringToken(String value) {
        super(TokenType.String);
        this.value = value;
    }
    public static StringToken of(String value) {
        return new StringToken(value);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof StringToken)) return false;
        if (!super.equals(o)) return false;
        StringToken that = (StringToken) o;
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
