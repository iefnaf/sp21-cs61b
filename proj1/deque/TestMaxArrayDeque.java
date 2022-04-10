package deque;

import org.junit.Test;
import java.util.Comparator;
import static org.junit.Assert.*;

/**
 * @author gfanfei@gmail.com
 * @date 2022/4/10 20:26
 */
public class TestMaxArrayDeque {
    @Test
    public void defaultComparatorTest() {
        MaxArrayDeque<Integer> mad = new MaxArrayDeque<>(new IntegerMaxComparator<>());
        mad.addLast(1);
        mad.addLast(9);
        mad.addLast(0);
        mad.addLast(0);
        assertEquals(mad.max(), Integer.valueOf(9));
    }

    @Test
    public void givenComparatorTest() {
        MaxArrayDeque<Integer> mad = new MaxArrayDeque<>(new IntegerMaxComparator<>());
        mad.addLast(1);
        mad.addLast(9);
        mad.addLast(0);
        mad.addLast(0);
        assertEquals(mad.max(new IntegerMinComparator<>()), Integer.valueOf(0));
    }

    private static class IntegerMaxComparator<T> implements Comparator<T> {

        @Override
        public int compare(T o1, T o2) {
            Integer i1 = (Integer) o1;
            Integer i2 = (Integer) o2;
            return i1.intValue() - i2.intValue();
        }
    }

    private static class IntegerMinComparator<T> implements Comparator<T> {

        @Override
        public int compare(T o1, T o2) {
            Integer i1 = (Integer) o1;
            Integer i2 = (Integer) o2;
            return i2.intValue() - i1.intValue();
        }
    }
}
