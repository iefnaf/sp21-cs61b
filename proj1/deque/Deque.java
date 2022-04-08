package deque;

import java.util.Iterator;

/**
 * Deque
 *
 * @author gfanfei@gmail.com
 * @date 2021.4.7 15:42
 */
public interface Deque<T> {

    /**
     * Adds an item of type T to the front of the deque.
     */
    void addFirst(T item);

    /**
     * Adds an item of type T to the back of the deque.
     */
    void addLast(T item);

    /**
     * Returns whether the deque is empty.
     */
    boolean isEmpty();

    /**
     * Returns the number of items in the deque.
     */
    int size();

    /**
     * Prints the items in the deque from first to last, separated by a space.
     * Once all the items have been printed, print out a new line.
     */
    void printDeque();

    /**
     * Removes and returns the item at the front of the deque.
     */
    T removeFirst();

    /**
     * Removes and returns the item at the last of the deque.
     */
    T removeLast();

    /**
     * Gets the item at the given index, where 0 is the front.
     * If no such item exists, return null.
     */
    T get(int index);

    Iterator<T> iterator();

    /**
     * Returns whether the parameter o is equal to the Deque.
     * o is considered equal if it is a Deque and if it contains the same
     * contents in the same order.
     */
    boolean equals(Object o);
}
