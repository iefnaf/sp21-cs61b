package deque;

import edu.princeton.cs.algs4.StdRandom;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * @author gfanfei@gmail.com
 * @date 2022/4/8 10:28
 */
public class ArrayDequeTest {
    @Test
    public void addIsEmptySizeTest() {
        ArrayDeque<String> ad1 = new ArrayDeque<>();
        assertTrue("A newly initialized ADeque should be empty", ad1.isEmpty());

        ad1.addFirst("front");
        assertEquals(1, ad1.size());
        assertFalse("ad1 should now contain 1 item", ad1.isEmpty());

        ad1.addLast("middle");
        assertEquals(2, ad1.size());

        ad1.addLast("back");
        assertEquals(3, ad1.size());

        System.out.println("Printing out deque: ");
        ad1.printDeque();
    }

    @Test
    public void addRemoveTest() {
        ArrayDeque<Integer> ad1 = new ArrayDeque<>();
        assertTrue("ad1 should be empty upon initialization", ad1.isEmpty());

        ad1.addFirst(10);
        assertFalse("ad1 should contain 1 item", ad1.isEmpty());

        ad1.removeLast();
        assertTrue("ad1 should be empty after removal", ad1.isEmpty());
    }

    @Test
    public void removeEmptyTest() {
        ArrayDeque<Integer> ad1 = new ArrayDeque<>();
        ad1.addFirst(3);

        ad1.removeLast();
        ad1.removeFirst();
        ad1.removeLast();
        ad1.removeFirst();

        assertEquals(0, ad1.size());
    }

    @Test
    public void multipleParamTest() {
        ArrayDeque<String> ad1 = new ArrayDeque<>();
        ArrayDeque<Double> ad2 = new ArrayDeque<>();
        ArrayDeque<Boolean> ad3 = new ArrayDeque<>();

        ad1.addFirst("string");
        ad2.addFirst(3.14);
        ad3.addFirst(true);

        ad1.removeFirst();
        ad2.removeFirst();
        ad3.removeFirst();
    }

    @Test
    public void emptyNullReturnTest() {
        ArrayDeque<Integer> ad1 = new ArrayDeque<>();

        assertEquals("Should return null when removeFirst is called on an empty Deque",
                null, ad1.removeFirst());
        assertEquals("Should return null when removeLast is called on an empty Deque",
                null, ad1.removeLast());
    }

    @Test
    public void bigLLDequeTest() {
        ArrayDeque<Integer> ad1 = new ArrayDeque<>();
        for (int i = 0; i < 1000000; i++) {
            ad1.addLast(i);
        }

        for (double i = 0; i < 500000; i++) {
            assertEquals("Should have the same value", i, (double) ad1.removeFirst(), 0.0);
        }

        for (double i = 999999; i > 500000; i--) {
            assertEquals("Should have the same value", i, (double) ad1.removeLast(), 0.0);
        }
    }

    @Test
    public void randomizedTest() {
        ArrayDeque<Integer> ad = new ArrayDeque<>();
        LinkedListDeque<Integer> ld = new LinkedListDeque<>();

        int N = 50000;
        for (int i = 0; i < N; i += 1) {
            int operationNumber = StdRandom.uniform(0, 8);
            if (operationNumber == 0) {
                // addFirst
                int randVal = StdRandom.uniform(0, 100);
                ad.addFirst(randVal);
                ld.addFirst(randVal);
            } else if (operationNumber == 1) {
                // addLast
                int randVal = StdRandom.uniform(0, 100);
                ad.addLast(randVal);
                ld.addLast(randVal);
            } else if (operationNumber == 2) {
                // isEmpty
                assertEquals(ad.isEmpty(), ld.isEmpty());
            } else if (operationNumber == 3) {
                // size
                assertEquals(ad.size(), ld.size());
            } else if (operationNumber == 4) {
                // removeFirst
                assertEquals(ad.removeFirst(), ld.removeFirst());
            } else if (operationNumber == 5) {
                // removeLast
                assertEquals(ad.removeLast(), ld.removeLast());
            } else if (operationNumber == 6) {
                // get
                assertEquals(ad.size(), ld.size());
                int size = ad.size();
                if (size > 0) {
                    int randIdx = StdRandom.uniform(0, size);
                    assertEquals(ad.get(randIdx), ld.get(randIdx));
                }
            }
        }
    }

    @Test
    public void iteratorTest() {
        ArrayDeque<String> ad = new ArrayDeque<>();
        String[] str = {"Danny", "Boodman", "T.D.Lemon", "1900"};
        for (String s : str) {
            ad.addLast(s);
        }
        int i = 0;
        for (String s : ad) {
            assertEquals(s, str[i]);
            i += 1;
        }
    }

    @Test
    public void equalsTest() {
        String[] strs = {"Danny", "Boodman", "T.D.Lemon", "1900"};
        ArrayDeque<String> ad1 = new ArrayDeque<>();
        ArrayDeque<String> ad2 = new ArrayDeque<>();
        LinkedListDeque<String> ld = new LinkedListDeque<>();
        ArrayDeque<String> ad3 = new ArrayDeque<>();
        for (String s : strs) {
            ad1.addLast(s);
            ad2.addLast(s);
            ld.addLast(s);
            ad3.addFirst(s);
        }
        assertTrue(ad1.equals(ad2));
        assertTrue(ad1.equals(ld));
        assertFalse(ad1.equals(ad3));
    }
}
