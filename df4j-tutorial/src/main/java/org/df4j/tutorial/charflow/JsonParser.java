package org.df4j.tutorial.charflow;

import org.df4j.core.dataflow.Actor;
import org.df4j.core.port.InpFlow;
import org.df4j.tutorial.charflow.jsontokens.Token;

public class JsonParser extends Actor {
    InpFlow<Token> inp = new InpFlow<>(this);

    @Override
    protected void runAction() {
        if (inp.isCompleted()) {
            onComplete();
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
                onError(new SyntaxError("bad token:"+t));
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
                onError(new SyntaxError("bad token:"+t));
        }
    }

    private void parseObject() {
        Token t = inp.current();
    }
}
