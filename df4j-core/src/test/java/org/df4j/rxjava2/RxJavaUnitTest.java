//package com.vogella.android.rxjava.simple;
package org.df4j.rxjava2;

import org.df4j.core.connector.messagescalar.CompletedPromise;
import org.junit.Test;

import io.reactivex.Observable;

import static junit.framework.Assert.assertTrue;


public class RxJavaUnitTest {
    String result="";

    // Simple subscription to a fix value
    @Test
    public void returnAValue(){
        result = "";
        Observable<String> observer = Observable.just("Hello"); // provides datea
        observer.subscribe(s -> result=s); // Callable as subscriber
        assertTrue(result.equals("Hello"));
    }

    @Test
    public void returnAValueDf(){
        result = "";
        CompletedPromise<String> observer = new CompletedPromise("Hello"); // provides datea
        observer.subscribe(s -> result=s); // Callable as subscriber
        assertTrue(result.equals("Hello"));
    }
}