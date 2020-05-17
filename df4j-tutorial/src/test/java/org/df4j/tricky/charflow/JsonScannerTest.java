package org.df4j.tricky.charflow;

import org.df4j.core.communicator.Completion;
import org.df4j.protocol.CharFlow;
import org.df4j.tricky.charflow.jsontokens.NumberToken;
import org.df4j.tricky.charflow.jsontokens.StringToken;
import org.df4j.tricky.charflow.jsontokens.Token;
import org.junit.Assert;
import org.junit.Test;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.ArrayList;

import static org.df4j.tricky.charflow.jsontokens.TokenType.*;

public class JsonScannerTest {
    static class TokenSinkArray extends Completion implements Subscriber<Token> {
        ArrayList<Token> tokens  = new ArrayList<>();
        Subscription sub;

        @Override
        public void onSubscribe(Subscription s) {
            sub = s;
            s.request(1);
        }

        @Override
        public void onError(Throwable t) {
            _complete(t);
        }

        @Override
        public void onComplete() {
            _complete(null);
        }

        @Override
        public void onNext(Token t) {
            tokens.add(t);
            sub.request(1);
        }

        public boolean equals(Token...expected) {
            if (expected.length != tokens.size()) {
                return false;
            }
            for (int k=0; k<expected.length; k++) {
                Token t1 = expected[k];
                Token t2 = tokens.get(k);
                if (!t1.equals(t2)) {
                    return false;
                }
            }
            return true;
        }
    }

    static class CharPublisherImpl implements CharFlow.CharPublisher {
        final String str;
        int pos = 0;
        CharSubscription subscription;

        CharPublisherImpl(String str) {
            this.str = str;
        }

        @Override
        public void subscribe(CharFlow.CharSubscriber subscriber) {
            subscription = new CharSubscription(subscriber);
            subscriber.onSubscribe(subscription);
        }

        class CharSubscription implements Subscription {
            final CharFlow.CharSubscriber subscriber;
            private boolean cancelled = false;

            public CharSubscription(CharFlow.CharSubscriber subscriber) {
                this.subscriber = subscriber;
            }

            @Override
            public void request(long n) {
                if (n <= 0) {
                    subscriber.onError(new IllegalArgumentException());
                    return;
                }
                if (cancelled) {
                    return;
                }
                for (;;) {
                    if (pos == str.length()) {
                        subscriber.onComplete();
                        cancel();
                        return;
                    }
                    char res = str.charAt(pos++);
                    subscriber.onNext(res);
                    if (--n == 0) {
                        return;
                    }
                }
            }

            @Override
            public void cancel() {
                cancelled = true;
            }
        }
    }

    private TokenSinkArray toScanner(String s) {
        CharPublisherImpl pub = new CharPublisherImpl(s);
        JsonScanner scanner = new JsonScanner();
        TokenSinkArray sink = new TokenSinkArray();
        pub.subscribe(scanner.inp);
        scanner.outp.subscribe(sink);
        scanner.start();
        boolean fin = scanner.blockingAwait(300);
        Assert.assertTrue(fin);
        fin = sink.blockingAwait(300);
        Assert.assertTrue(fin);
        return sink;
    }

    @Test
    public void emptyArrayTest() {
        TokenSinkArray sink = toScanner("[]");
        boolean condition = sink.equals(Token.of(LeftBracket), Token.of(RightBracket));
        Assert.assertTrue(condition);
    }

    @Test
    public void arrayTest1() {
        TokenSinkArray sink = toScanner("[null]");
        boolean condition = sink.equals(Token.of(LeftBracket), Token.of(Null), Token.of(RightBracket));
        Assert.assertTrue(condition);
    }

    @Test
    public void arrayTest() {
        TokenSinkArray sink = toScanner("[ \"abc\":true,false, 123,]");
        boolean condition = sink.equals(Token.of(LeftBracket), StringToken.of("abc"), Token.of(Semicolon), Token.of(True), Token.of(Comma), Token.of(False)
                ,Token.of(Comma), NumberToken.of(123),Token.of(Comma), Token.of(RightBracket));
        Assert.assertTrue(condition);
    }
}
