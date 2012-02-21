package com.github.rfqu.df4j.ioexample.dock;

import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Hashtable;

import jdbm.RecordManager;
import jdbm.RecordManagerFactory;
import jdbm.btree.BTree;

public class JdbmTest {
    public static final String testFolder = System.getProperty("java.io.tmpdir",".") + "/_testdb";

    static public String newTestFile(){
        return testFolder+ File.separator + "test"+System.nanoTime();
    }

    static public RecordManager newRecordManager() throws IOException{
        return RecordManagerFactory.createRecordManager(newTestFile());
    }
    
    RecordManager recman;
    /**
     * Test w/o compression or specialized key or value serializers.
     * 
     * @throws IOException
     */
    // TODO[cdg] Fix this test
    //@Ignore   

    public void test_001() throws IOException {
        recman = newRecordManager();
        BTree<Long,Long> tree = BTree.createInstance( recman);
        doTest( recman, tree, 501 );
        recman.close();
    }
    
    
    public static void doTest( RecordManager recman, BTree<Long,Long> tree, int ITERATIONS )
        throws IOException
    {

        long beginTime = System.currentTimeMillis();
        Hashtable<Long,Long> hash = new Hashtable<Long,Long>();

        for ( int i=0; i<ITERATIONS; i++) {
            Long random = new Long( random( 0, 64000 ) );

            if ( ( i % 500 ) == 0 ) {
                long elapsed = System.currentTimeMillis() - beginTime;
                System.out.println( "Iterations=" + i + " Objects=" + tree.size()+", elapsed="+elapsed+"ms" );
                recman.commit();
            }
            if ( hash.get( random ) == null ) {
                //System.out.println( "Insert " + random );
                hash.put( random, random );
                tree.insert( random, random, true );
            } else {
                //System.out.println( "Remove " + random );
                //hash.remove( random );
                Object removed = (Object) tree.find( random );
                if ( ( removed == null ) || ( ! removed.equals( random ) ) ) {
                    throw new IllegalStateException( "Remove expected " + random + " got " + removed );
                }
            }
            // tree.assertOrdering();
            compare( tree, hash );
        }

    }
    
    static long random( int min, int max ) {
        return Math.round( Math.random() * ( max-min) ) + min;
    }

    static void compare( BTree<Long,Long> tree, Hashtable<Long,Long> hash ) throws IOException {
        boolean failed = false;
        Enumeration<Long> enumeration;

        if ( tree.size() != hash.size() ) {
            throw new IllegalStateException( "Tree size " + tree.size() + " Hash size " + hash.size() );
        }

        enumeration = hash.keys();
        while ( enumeration.hasMoreElements() ) {
            Long key = enumeration.nextElement();
            Long hashValue = hash.get( key );
            Long treeValue = tree.find( key );
            if ( ! hashValue.equals( treeValue ) ) {
                System.out.println( "Compare expected " + hashValue + " got " + treeValue );
                failed = true;
            }
        }
        if ( failed ) {
            throw new IllegalStateException( "Compare failed" );
        }
    }

    public static void main(String[] args) throws IOException {
        new JdbmTest().test_001();
    }

}
