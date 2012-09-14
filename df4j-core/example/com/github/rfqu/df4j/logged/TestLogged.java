package com.github.rfqu.df4j.logged;

import java.util.Arrays;
import java.util.Iterator;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;

/**
 * this is a test for interfaces
 * 
 * @author kaigorodov
 *
 */
public class TestLogged {
	static void restart(Iterable<Transaction> txs, Engine en) {
		for (Transaction tx: txs) {
		    Backword<Action> actions = new Backword<Action>(tx.actions);
	        for (Action act: actions) {
	            act.act(en);
	        }		    
		}
	}
	
	/**
	 * @param args
	 */
    public static void main(String[] args) {
        Engine en = new Engine();
        Transaction[] txarr=testData();
        Backword<Transaction> log = new Backword<Transaction>(txarr);
        restart(log, en);
    }

    static Transaction[] testData() {
        return new Transaction[] {
             new Transaction(new Action[] {
                  new SendAction(1, "m1"),
                  new Action(),
              }),
              new Transaction(new Action[] {
                   new Action(),
                   new Action(),
              }),
        };
    }

}

/**
 * backword scan
 *
 */
class Backword<T> implements Iterable<T> {
	T[] arr;
	
	public Backword(T[] arr) {
		this.arr = arr;
	}

	@Override
	public Iterator<T> iterator() {
		return new Iterator<T>(){
		    int i=arr.length; // count
		    
            @Override
            public boolean hasNext() {
                return i>0;
            }

            @Override
            public T next() {
                return arr[--i];
            }

            @Override
            public void remove() {
                throw new NotImplementedException();
            }
		    
		};
	}
	
}

class Transaction implements Iterable<Action> {
	Action[] actions;

	public Transaction(Action[] actions) {
		super();
		this.actions = actions;
	}

	@Override
	public Iterator<Action> iterator() {
		// TODO Auto-generated method stub
		return null;
	}
	
}

abstract class Action {

    public abstract void act(Engine en);
}

class SendAction extends Action {
    long port;
    Object message;
    
    public SendAction(long port, Object message) {
        this.port = port;
        this.message = message;
    }

    public void act(Engine en) {
        en.send(port, message);
    }
	
}

class DeleteAction extends Action {
    public void act(Engine en) {
        // TODO Auto-generated method stub
        
    }
	
}

class Engine {

    public void send(long port, Object message) {
        // TODO Auto-generated method stub
        
    }
    
}

