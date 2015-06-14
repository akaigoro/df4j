package org.df4j.core;

import static org.junit.Assert.*;
import org.df4j.core.Actor.StreamInput;
import org.junit.Test;

public class SharedPlaceTest {
    
    static class ShPlace extends SharedPlace<ShPlace> {
        StreamInput<R> resourses = new StreamInput<R>();

        @Override
        protected void act(Port<ShPlace> port) throws Exception {
            port.post(resourses.get());
        }
        
        public void ret(R token) {
            resourses.post(token);
        }

        int id=44;
        {resourses.post(this);}
        
        public void ret() {
            ret(this);
        }

        @Override
        public void ret(ShPlace token) {
            // TODO Auto-generated method stub
            
        }

        @Override
        protected void act(Port<ShPlace> message) throws Exception {
            // TODO Auto-generated method stub
            
        }
    }

    @Test
    public void test() {
        ShPlace place=new ShPlace();
        Port<ShPlace> consumer=(reply) -> {
            System.out.println("id="+reply.id);
            assertEquals(44, reply.id);
            reply.ret();
        };
        place.post(consumer);
        place.post(consumer);
    }

}
