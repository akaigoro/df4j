package dffw;

import java.util.concurrent.atomic.AtomicLong;

import org.junit.Ignore;
import org.junit.Test;

/***************************************************************************
 * Quicksort code from Sedgewick 7.1, 7.2.
 **************************************************************************/
public class QuickSort {
    int maxThrCount=4;
    int thrCount=1;
    private boolean par=false;
    AtomicLong ptime=new AtomicLong(0);
    AtomicLong qstime=new AtomicLong(0);
    
      public void quicksort(double[] a) {
        quicksort(a, 0, a.length - 1, 0);
      }

    void quicksort(final double[] a, final int left, final int right, final int tdepth) {
        if (right <= left)
            return;
        long start = System.currentTimeMillis();
        final int i = partition(a, left, right);
        long cont = System.currentTimeMillis();
        long stop =0;
//        if (tdepth==0)
        ptime.addAndGet(cont-start);

        if (par && (tdepth < 1) && ((i - left) > 1000)) {
            final Thread t = new Thread() {
                public void run() {
                    quicksort(a, left, i - 1, tdepth + 1);
                }
            };
            t.start();
            thrCount++;
            quicksort(a, i + 1, right, tdepth + 1);
            stop = System.currentTimeMillis();

            try {
                t.join();
            } catch (InterruptedException e) {
                throw new RuntimeException("Cancelled", e);
            }
        } else {
            quicksort(a, left, i - 1, tdepth+1);
            quicksort(a, i + 1, right, tdepth+1);
            stop = System.currentTimeMillis();
        }
//        if (tdepth==0)
        qstime.addAndGet(stop-cont);
    }

    // partition a[left] to a[right], assumes left < right
    int partition(double[] a, int left, int right) {
        int i = left - 1;
        int j = right;
        while (true) {
//          while (less(a[++i], a[right]))
            while (a[++i]<a[right])
                // find item on left to swap
                ; // a[right] acts as sentinel
//          while (less(a[right], a[--j]))
            while (a[right]<a[--j])
                // find item on right to swap
                if (j == left)
                    break; // don't go out-of-bounds
            if (i >= j)
                break; // check if pointers cross
            exch(a, i, j); // swap two elements into place
        }
        exch(a, i, right); // swap with partition element
        return i;
    }

    // is x < y ?
    private static boolean less(double x, double y) {
        return (x < y);
    }

    // exchange a[i] and a[j]
    private static void exch(double[] a, int i, int j) {
        double swap = a[i];
        a[i] = a[j];
        a[j] = swap;
    }

    @Test
    @Ignore
    public void t01() {
        int N = 5000000; // Integer.parseInt(args[0]);
        double[] a = new double[N];

        generate(N, a);

        // sort them
        long start = System.currentTimeMillis();
        quicksort(a);
        long stop = System.currentTimeMillis();
        long elapsed = (stop - start);
        long ptime = this.ptime.longValue();
        long qstime = this.qstime.longValue();
        System.out.println("Quicksort:   " + elapsed + " ms; partTime="+ptime+"; qsTime="+qstime+"; thrCount=" + thrCount);
    }

    @Test
    @Ignore
    public void t02() {
        par = true;
        t01();
    }

    private void generate(int N, double[] a) {
        // generate N random real numbers between 0 and 1
        long start = System.currentTimeMillis();
        for (int i = 0; i < N; i++)
            a[i] = Math.random();
        long stop = System.currentTimeMillis();
        long elapsed = (stop - start);
        System.out.println("Generating input:  " + elapsed + " ms");
    }

    // test client
    public static void main(String[] args) {
        new QuickSort().t01();
    }

}
