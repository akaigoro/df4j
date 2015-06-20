package org.df4j.core.actor;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.concurrent.Executor;

import org.df4j.core.ext.SerialExecutor;
import org.df4j.core.ExceptionHandler;

/**
 * Actor with arbitrary interface
 * 
 * @author rfq
 *
 * @param <I>
 */

public class ProxyActor {
    
	public static <I>I makeProxy(java.lang.Class<I> intrf, I proxied, Executor executor) {
        SerialExecutor execActor = new SerialExecutor(executor);
    	return _makeProxy(intrf, proxied, execActor);
    }

	public static <I>I makeProxy(Class<I> intrf, I proxied) {
        SerialExecutor execActor = new SerialExecutor();
    	return _makeProxy(intrf, proxied, execActor);
    }
	
	@SuppressWarnings("unchecked")
	private static <I> I _makeProxy(Class<I> intrf, I proxied, SerialExecutor execActor) {
		return (I) Proxy.newProxyInstance(proxied.getClass().getClassLoader(),
    	        new Class[] { intrf }, new ActorInvocationHandler(proxied, execActor));
	}
    
	static class ActorInvocationHandler implements InvocationHandler {
		protected final Object proxied;
	    protected final SerialExecutor execActor;

	    public ActorInvocationHandler(Object proxied, SerialExecutor execActor) {
			this.proxied = proxied;
			this.execActor = execActor;
		}

		public Object invoke(Object proxy, final Method method, final Object[] args) throws Throwable {
            execActor.execute(new Runnable(){
                @Override
                public void run() {
					try {
						method.invoke(proxied, args);
					} catch (IllegalAccessException | IllegalArgumentException e) {
						handleException(e);
					} catch (InvocationTargetException e) {
						handleException(e.getCause());
					}
                }

				private void handleException(Throwable e) {
					if (proxied instanceof ExceptionHandler) {
						((ExceptionHandler)proxied).handleException(e);
					} else {
						e.printStackTrace();
					}
				}
            
            });
            return null;
	    }
	}

	/**
	 * throws checked exception as if it is unchecked
	 * convenient to use instead of explicit throw in proxied methods - otherwise
	 * interface methods should have been declared as throwing exceptionsm which is
	 * false and incovinient for interface side.
	 * taken from http://blog.ragozin.info/2011/10/java-how-to-throw-undeclared-checked.html
	 * @param e
	 */
    public static void throwUncheked(Throwable e) {
    	ProxyActor.<RuntimeException>throwAny(e);
    }
   
    @SuppressWarnings("unchecked")
    private static <E extends Throwable> void throwAny(Throwable e) throws E {
        throw (E)e;
    }
}