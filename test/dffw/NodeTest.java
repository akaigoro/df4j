package dffw;

import static org.junit.Assert.assertEquals;

import java.util.concurrent.ExecutionException;

import org.junit.Test;

//import dffw.Node.Port;


public class NodeTest {
    DFExecutor e=new DFExecutor();

    /**
     * compute 2*3
     * @throws ExecutionException 
     * @throws InterruptedException 
     */
    @Test
    public void t01() throws  ExecutionException, InterruptedException {
        
        Mult m1=new Mult();
        m1.p1.put(2);
        m1.p2.put(3);

        Integer res=m1.get();
//        System.out.println("res="+res);
        assertEquals(6, res.intValue());
    }

    /**
     * compute 2*3+4*5=26
     * @throws Exception 
     */
   @Test
    public void t02() throws Exception {
        
        Mult m1=new Mult();
        m1.p1.put(2);
        m1.p2.put(3);
        Mult m2=new Mult();
        m2.p1.put(4);
        m2.p2.put(5);
        Adder a1=new Adder();
        m1.connect(a1.p1);
        m2.connect(a1.p2);
        Integer res=a1.get();
        System.out.println("res="+res);
    }

   /**
    * compute 2*3+4*5=26
    * @throws Exception 
    */
  @Test
   public void t03() throws Exception {
       Integer res=new Adder(new Mult(2, 3), new Mult(4, 5)).get();
       System.out.println("res="+res);
   }

    abstract class BinaryOP<T> extends Node<T> {
        public BinaryOP() {
            super(e);
        }
        Port<T> p1=new Port<T>(); 
        Port<T> p2=new Port<T>();
        
        public BinaryOP(T operand1, T operand2) {
            this();
            p1.put(operand1);
            p2.put(operand2);
        }

        public BinaryOP(Node<T> source1, Node<T> source2) throws ExecutionException {
            this();
            source1.connect(p1);
            p2.connect(source2);
        }

    }

    class Adder extends BinaryOP<Integer> {
        public Adder() {
        }
        public Adder(Integer operand1, Integer operand2) {
            super(operand1, operand2);
        }

        public Adder(Node<Integer> source1, Node<Integer> source2) throws ExecutionException {
            super(source1, source2);
        }

        public Integer call() {
            return p1.get()+p2.get();
        }
    }
    
    class Mult extends BinaryOP<Integer> {
        public Mult() {
        }
        public Mult(Integer operand1, Integer operand2) {
            super(operand1, operand2);
        }

        public Mult(Node<Integer> source1, Node<Integer> source2) throws ExecutionException {
            super(source1, source2);
        }

        public Integer call() {
            return p1.get()*p2.get();
        }
    }}
