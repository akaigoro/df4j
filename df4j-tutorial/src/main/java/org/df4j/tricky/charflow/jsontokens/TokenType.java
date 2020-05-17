package org.df4j.tricky.charflow.jsontokens;

import java.util.HashMap;

public enum TokenType {
    LeftBrace('{'),
    RightBrace('}'),
    LeftBracket('['),
    RightBracket(']'),
    Comma(','),
    Semicolon(':'),
    True,
    False,
    Null,
    String,
    Number;

    private final char c;

    TokenType() {
        this.c = 0;
    }
    TokenType(char c) {
        this.c = c;
    }

    public static final HashMap<Character, TokenType> letterTypes = new HashMap<>();
    static {
        for (TokenType type : TokenType.values()) {
            if (type.c != 0) {
                letterTypes.put(type.c, type);
            }
        }
    }
}
