package deque;

import java.util.Iterator;

/**
 * ArrayDeque
 *
 * @author gfanfei@gmail.com
 * @date 2021.4.7 15:52
 */
public class ArrayDeque<T> implements Deque<T> {
    private T[] items;
    private int size;
    private int nextFirst;
    private int nextLast;
    private static final int START_SIZE = 8;
    private static final double LOAD_FACTOR = 0.25;

    public ArrayDeque() {
        items = (T []) new Object[START_SIZE];
        nextFirst = 0;
        nextLast = 1;
        size = 0;
    }

    @Override
    public void addFirst(T item) {
        if (size == items.length) {
            resize(items.length * 2);
        }
        items[nextFirst] = item;
        size += 1;
        nextFirst = (nextFirst - 1 + items.length) % items.length;
    }

    @Override
    public void addLast(T item) {
        if (size == items.length) {
            resize(size * 2);
        }
        items[nextLast] = item;
        size += 1;
        nextLast = (nextLast + 1) % items.length;
    }

    @Override
    public boolean isEmpty() {
        return size == 0;
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public void printDeque() {
        int idx = (nextFirst + 1) % items.length;
        while (idx != nextLast) {
            System.out.print(items[idx] + " ");
            idx = (idx + 1) % items.length;
        }
        System.out.println();
    }

    @Override
    public T removeFirst() {
        if (isEmpty()) {
            return null;
        }
        nextFirst = (nextFirst + 1) % items.length;
        T res = items[nextFirst];
        items[nextFirst] = null;
        size -= 1;
        if (size >  START_SIZE && ((double) size / items.length) < LOAD_FACTOR) {
            resize(items.length / 2);
        }
        return res;
    }

    @Override
    public T removeLast() {
        if (isEmpty()) {
            return null;
        }
        nextLast = (nextLast - 1 + items.length) % items.length;
        T res = items[nextLast];
        items[nextLast] = null;
        size -= 1;
        if (size >  START_SIZE && ((double) size / items.length) < LOAD_FACTOR) {
            resize(items.length / 2);
        }
        return res;
    }

    @Override
    public T get(int index) {
        if (isEmpty()) {
            return null;
        }
        if (index < 0 || index >= size) {
            return null;
        }
        int first = (nextFirst + 1) % items.length;
        int last = (nextLast - 1 + items.length) % items.length;
        int idx = 0;
        if (first <= last) {
            idx = first + index;
        } else {
            if (index <= (items.length - first - 1)) {
                idx = first + index;
            } else {
                idx = index - (items.length - first);
            }
        }
        return items[idx];
    }

    @Override
    public Iterator<T> iterator() {
        return null;
    }

    private void resize(int capacity) {
        T[] a = (T []) new Object[capacity];

        int first = (nextFirst + 1) % items.length;
        int last = (nextLast - 1 + items.length) % items.length;

        if (first <= last) {
            System.arraycopy(items, first, a, 0, size);
        } else {
            int len1 = items.length - first;
            System.arraycopy(items, first, a, 0, len1);
            System.arraycopy(items, 0, a, len1, last + 1);
        }
        items = a;
        nextFirst = capacity - 1;
        nextLast = size;
    }
}
