package randomizedtest;

import edu.princeton.cs.algs4.StdRandom;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Created by hug.
 */
public class TestBuggyAList {
    @Test
    public void testThreeAddThreeRemove() {
        AListNoResizing<Integer> aList = new AListNoResizing<>();
        BuggyAList<Integer> bList = new BuggyAList<>();
        for (int i = 0; i < 5; i += 1) {
            aList.addLast(i);
            bList.addLast(i);
        }
        for (int i = 0; i < 5; i += 1) {
            assertEquals(aList.removeLast(), bList.removeLast());
        }
    }

    @Test
    public void randomizedTest() {
        AListNoResizing<Integer> L = new AListNoResizing<>();
        BuggyAList<Integer> buggyL = new BuggyAList<>();

        int N = 5000;
        for (int i = 0; i < N; i += 1) {
            int operationNumber = StdRandom.uniform(0, 4);
            if (operationNumber == 0) {
                // addLast
                int randVal = StdRandom.uniform(0, 100);
                L.addLast(randVal);
                buggyL.addLast(randVal);
            } else if (operationNumber == 1) {
                // size
                int size = L.size();
                int bsize = buggyL.size();
                assertEquals(size, bsize);
            } else if (operationNumber == 2) {
                assertEquals(L.size(), buggyL.size());
                // getLast
                if (L.size() > 0) {
                    int lastVal = L.getLast();
                    int blastVal = buggyL.getLast();
                    assertEquals(lastVal, blastVal);
                }
            } else if (operationNumber == 3) {
                assertEquals(L.size(), buggyL.size());
                // removeLast
                if (L.size() > 0) {
                    int lastVal = L.removeLast();
                    int blastVal = buggyL.removeLast();
                    assertEquals(lastVal, blastVal);
                }
            }
        }
    }
}
