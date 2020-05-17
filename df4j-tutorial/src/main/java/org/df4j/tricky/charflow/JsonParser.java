package org.df4j.tricky.charflow;

import org.df4j.core.dataflow.Actor;
import org.df4j.core.port.InpFlow;
import org.df4j.tricky.charflow.jsontokens.Token;

public class JsonParser extends Actor {
    InpFlow<Token> inp = new InpFlow<>(this);

    @Override
    protected void runAction() {
        if (inp.isCompleted()) {
            complete();
            return;
        }
        Token t = inp.current();
        switch (t.type) {
            case LeftBracket:
                inp.remove();
                nextAction(this::parseArray);
                break;
            case LeftBrace:
                inp.remove();
                nextAction(this::parseObject);
                break;
            default:
                completeExceptionally(new SyntaxError("bad token:"+t));
        }

    }

    private void parseArray() {
        Token t = inp.current();
        switch (t.type) {
            case LeftBracket:
                inp.remove();
                nextAction(this::parseArray);
                break;
            case LeftBrace:
                inp.remove();
                nextAction(this::parseObject);
                break;
            default:
                completeExceptionally(new SyntaxError("bad token:"+t));
        }
    }

    private void parseObject() {
        Token t = inp.current();
    }
}
