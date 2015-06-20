package org.df4j.core;

/**
 * 
 * @author Jim
 *
 */
public class SharedToken<T extends SharedToken<T>> extends SharedPlace<T> {
    protected Semafor sema=new Semafor();
    
    {sema.up();}

    @SuppressWarnings("unchecked")
    @Override
    protected void act(Port<T> port) throws Exception {
        port.post((T) this);
    }

    public void ret() {
        sema.up();
    }

    @Override
    public void ret(T token) {
        if (token!=this) {
            throw new IllegalArgumentException("wrong token");
        }
        ret();
    }
    
}
