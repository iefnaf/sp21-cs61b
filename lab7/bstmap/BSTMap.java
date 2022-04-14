package bstmap;

import java.util.*;

/**
 * @author gfanfei@gmail.com
 * @date 2022/4/13 17:17
 */
public class BSTMap<K extends Comparable<K>, V> implements Map61B<K, V> {

    private BSTNode root;

    public BSTMap() {
    }

    @Override
    public void clear() {
        root = null;
    }

    @Override
    public boolean containsKey(K key) {
        return containsKey(root, key);
    }

    private boolean containsKey(BSTNode x, K key) {
        if (x == null) {
            return false;
        }
        if (key == null) {
            throw new IllegalArgumentException("calls containsKey() with a null key");
        }
        int cmp = x.key.compareTo(key);
        if (cmp < 0) {
            return containsKey(x.right, key);
        } else if (cmp > 0) {
            return containsKey(x.left, key);
        } else {
            return true;
        }
    }

    @Override
    public V get(K key) {
        return get(root, key);
    }

    private V get(BSTNode x, K key) {
        if (x == null) {
            return null;
        }
        if (key == null) {
            throw new IllegalArgumentException("calls get() with a null key");
        }
        int cmp = x.key.compareTo(key);
        if (cmp < 0) {
            return get(x.right, key);
        } else if (cmp > 0) {
            return get(x.left, key);
        } else {
            return x.val;
        }
    }

    @Override
    public int size() {
        return size(root);
    }

    private int size(BSTNode x) {
        if (x == null) {
            return 0;
        }
        return x.size;
    }

    @Override
    public void put(K key, V value) {
        root = put(root, key, value);
    }

    private BSTNode put(BSTNode x, K key, V value) {
        if (x == null) {
            return new BSTNode(key, value, 1);
        }
        int cmp = x.key.compareTo(key);
        if (cmp < 0) {
            x.right = put(x.right, key, value);
        } else if (cmp > 0) {
            x.left = put(x.left, key, value);
        } else {
            x.val = value;
        }
        x.size = size(x.left) + size(x.right) + 1;
        return x;
    }

    @Override
    public Set<K> keySet() {
        Iterator<K> iter = iterator();
        Set<K> set = new TreeSet<>();
        while (iter.hasNext()) {
            set.add(iter.next());
        }
        return set;
    }

    @Override
    public V remove(K key) {
        V res = get(key);
        if (res == null) {
            return null;
        }
        root = remove(root, key);
        return res;
    }

    private BSTNode remove(BSTNode x, K key) {
        if (x == null) {
            return null;
        }
        int cmp = x.key.compareTo(key);
        if (cmp < 0) {
            x.right = remove(x.right, key);
        } else if (cmp > 0) {
            x.left = remove(x.left, key);
        } else {
            // we've found key
            if (x.left == null) {
                return x.right;
            } else if (x.right == null) {
                return x.left;
            } else {
                // use the smallest node in root.right as the new root
                // its left is root's left
                // its right is root's right after remove the smallest node
                BSTNode t = x;
                x = min(t.right);
                x.right = deleteMin(t.right);
                x.left = t.left;
            }
        }
        x.size = size(x.left) + size(x.right) + 1;
        return x;
    }

    /**
     * Return the smallest node in the tree.
     */
    private BSTNode min(BSTNode x) {
        if (x == null) {
            return null;
        }
        if (x.left == null) {
            return x;
        } else {
            return min(x.left);
        }
    }

    /**
     * Removes the smallest key and associated value from the tree.
     */
    private BSTNode deleteMin(BSTNode x) {
        if (x.left == null) {
            return x.right;
        }
        x.left = deleteMin(x.left);
        x.size = size(x.left) + size(x.right) + 1;
        return x;
    }


    @Override
    public V remove(K key, V value) {
        V v = get(key);
        if (v == null || !v.equals(value)) {
            return null;
        }
        remove(key);
        return value;
    }

    @Override
    public Iterator<K> iterator() {
        return new BSTMapIterator();
    }

    private class BSTMapIterator implements Iterator<K> {
        Queue<K> queue;

        BSTMapIterator() {
            queue = new ArrayDeque<>();
            inorder(root);
        }

        private void inorder(BSTNode x) {
            if (x == null) {
                return;
            }
            inorder(x.left);
            queue.add(x.key);
            inorder(x.right);
        }

        @Override
        public boolean hasNext() {
            return !queue.isEmpty();
        }

        @Override
        public K next() {
            return queue.remove();
        }
    }

    public void printInOrder() {
        for (K k : this) {
            System.out.print(k + " ");
        }
    }

    private class BSTNode {
        private K key;
        private V val;
        private int size;
        private BSTNode left, right;

        BSTNode(K key, V val, int size) {
            this.key = key;
            this.val = val;
            this.size = size;
        }
    }
}
