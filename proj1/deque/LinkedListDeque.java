package deque;

import java.util.Iterator;

/**
 * Linked List Deque
 *
 * @author gfanfei@gmail.com
 * @date 2021.4.7 15:42
 */
public class LinkedListDeque<T> implements Deque<T> {
    private int size;
    private LinkedListNode sentFront;
    private LinkedListNode sentBack;

    private class LinkedListNode {
        private T item;
        private LinkedListNode prev;
        private LinkedListNode next;

        LinkedListNode(T i, LinkedListNode p, LinkedListNode n) {
            item = i;
            prev = p;
            next = n;
        }
    }

    public LinkedListDeque() {
        sentFront = new LinkedListNode(null, null, null);
        sentBack = new LinkedListNode(null, null, null);
        sentFront.next = sentBack;
        sentBack.prev = sentFront;
        size = 0;
    }

    @Override
    public void addFirst(T item) {
        LinkedListNode newNode = new LinkedListNode(item, sentFront, sentFront.next);
        sentFront.next.prev = newNode;
        sentFront.next = newNode;
        size += 1;
    }

    @Override
    public void addLast(T item) {
        LinkedListNode newNode = new LinkedListNode(item, sentBack.prev, sentBack);
        sentBack.prev.next = newNode;
        sentBack.prev = newNode;
        size += 1;
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
        LinkedListNode node = sentFront.next;
        System.out.print(node.item);
        node = node.next;
        while (node != sentBack) {
            System.out.print(" " + node.item);
            node = node.next;
        }
        System.out.println();
    }

    @Override
    public T removeFirst() {
        if (isEmpty()) {
            return null;
        }
        LinkedListNode node = sentFront.next;
        node.next.prev = node.prev;
        node.prev.next = node.next;
        size -= 1;
        return node.item;
    }

    @Override
    public T removeLast() {
        if (isEmpty()) {
            return null;
        }
        LinkedListNode node = sentBack.prev;
        node.next.prev = node.prev;
        node.prev.next = node.next;
        size -= 1;
        return node.item;
    }

    @Override
    public T get(int index) {
        if (index >= size()) {
            return null;
        }
        LinkedListNode node = sentFront;
        for (int i = 0; i < index; i += 1) {
            node = node.next;
        }
        return node.item;
    }

    public T getRecursive(int index) {
        if (index >= size()) {
            return null;
        }
        return getRecursiveHelper(sentFront.next, index);
    }

    private T getRecursiveHelper(LinkedListNode head, int index) {
        if (index == 0) {
            return head.item;
        }
        return getRecursiveHelper(head.next, index - 1);
    }

    @Override
    public Iterator<T> iterator() {
        return null;
    }
}
