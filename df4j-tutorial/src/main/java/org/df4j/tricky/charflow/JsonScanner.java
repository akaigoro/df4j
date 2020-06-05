package org.df4j.tricky.charflow;

import org.df4j.tricky.charflow.jsontokens.NumberToken;
import org.df4j.tricky.charflow.jsontokens.StringToken;
import org.df4j.tricky.charflow.jsontokens.Token;
import org.df4j.tricky.charflow.jsontokens.TokenType;

public class JsonScanner extends Scanner {
    StringBuilder sb;
    int number;

    @Override
    protected void runAction() {
        selector();
    }

    void selector() {
        if (inp.isCompleted()) {
            outp.onComplete();
            complete();
            return;
        }
        char c = inp.current();
        TokenType type = TokenType.letterTypes.get(c);
        if (type != null) {
            inp.remove();
            Token t = new Token(type);
            outp.onNext(t);
        } else {
            if (c == '"') {
                inp.remove();
                sb = new StringBuilder();
                nextAction(this::stringBody);
            } else if (Character.isDigit(c)) {
                inp.remove();
                number = c-'0';
                nextAction(this::numberBody);
            } else if (Character.isLetter(c)) {
                sb = new StringBuilder();
                nextAction(this::namedValue);
            } else if (Character.isSpaceChar(c)) {
                inp.remove();
            } else {
                throw new SyntaxError("unexpected character: " + c);
            }
        }
    }

    void stringBody() {
        char c = inp.current();
        if (c != '"') {
            sb.append(c);
            inp.remove();
        } else {
            inp.remove();
            String value = sb.toString();
            Token t = new StringToken(value);
            outp.onNext(t);
            sb = null;
            nextAction(this::selector);
        }
    }
    void numberBody() {
        char c = inp.current();
        if (Character.isDigit(c)) {
            inp.remove();
            number=number*10+(c-'0');
        } else {
            Token t = new NumberToken(number);
            outp.onNext(t);
            nextAction(this::selector);
        }
    }

    void namedValue() {
        char c = inp.current();
        if (Character.isLetter(c)) {
            inp.remove();
            sb.append(c);
        } else {
            Token token;
            String word = sb.toString();
            String str = word.toLowerCase();
            switch (str) {
                case "true":
                    token = Token.of(TokenType.True);
                    break;
                case "false":
                    token = Token.of(TokenType.False);
                    break;
                case "null":
                    token = Token.of(TokenType.Null);
                    break;
                default:
                    throw new SyntaxError("unexpected word: " + word);
            }
            sb = null;
            outp.onNext(token);
            nextAction(this::selector);
        }
    }
}
