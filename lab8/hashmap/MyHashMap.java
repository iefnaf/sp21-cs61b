package hashmap;

import java.util.*;

/**
 *  A hash table-backed Map implementation. Provides amortized constant time
 *  access to elements via get(), remove(), and put() in the best case.
 *
 *  Assumes null keys will never be inserted, and does not resize down upon remove().
 *  @author gfanfei@gmail.com
 */
public class MyHashMap<K, V> implements Map61B<K, V> {

    /**
     * Protected helper class to store key/value pairs
     * The protected qualifier allows subclass access
     */
    protected class Node {
        K key;
        V value;

        Node(K k, V v) {
            key = k;
            value = v;
        }
    }

    /* Instance Variables */
    private Collection<Node>[] buckets;
    // You should probably define some more!
    private static final int DEFAULT_SIZE = 16;
    private static final double DEFAULT_LOAD_FACTOR = 0.75;
    private final double loadFactor;
    private final HashSet<K> keys;

    /** Constructors */
    public MyHashMap() {
        this(DEFAULT_SIZE, DEFAULT_LOAD_FACTOR);
    }

    public MyHashMap(int initialSize) {
        this(initialSize, DEFAULT_LOAD_FACTOR);
    }

    /**
     * MyHashMap constructor that creates a backing array of initialSize.
     * The load factor (# items / # buckets) should always be <= loadFactor
     *
     * @param initialSize initial size of backing array
     * @param maxLoad maximum load factor
     */
    public MyHashMap(int initialSize, double maxLoad) {
        this.loadFactor = maxLoad;
        buckets = createTable(initialSize);
        for (int i = 0; i < buckets.length; i++) {
            buckets[i] = createBucket();
        }
        keys = new HashSet<>();
    }

    /**
     * Returns a new node to be placed in a hash table bucket
     */
    private Node createNode(K key, V value) {
        return new Node(key, value);
    }

    /**
     * Returns a data structure to be a hash table bucket
     *
     * The only requirements of a hash table bucket are that we can:
     *  1. Insert items (`add` method)
     *  2. Remove items (`remove` method)
     *  3. Iterate through items (`iterator` method)
     *
     * Each of these methods is supported by java.util.Collection,
     * Most data structures in Java inherit from Collection, so we
     * can use almost any data structure as our buckets.
     *
     * Override this method to use different data structures as
     * the underlying bucket type
     *
     * BE SURE TO CALL THIS FACTORY METHOD INSTEAD OF CREATING YOUR
     * OWN BUCKET DATA STRUCTURES WITH THE NEW OPERATOR!
     */
    protected Collection<Node> createBucket() {
        return new LinkedList<>();
    }

    /**
     * Returns a table to back our hash table. As per the comment
     * above, this table can be an array of Collection objects
     *
     * BE SURE TO CALL THIS FACTORY METHOD WHEN CREATING A TABLE SO
     * THAT ALL BUCKET TYPES ARE OF JAVA.UTIL.COLLECTION
     *
     * @param tableSize the size of the table to create
     */
    private Collection<Node>[] createTable(int tableSize) {
        return new Collection[tableSize];
    }

    @Override
    public void clear() {
        for (Collection<Node> bucket : buckets) {
            bucket.clear();
        }
        keys.clear();
    }

    @Override
    public boolean containsKey(K key) {
        return keys.contains(key);
    }

    @Override
    public V get(K key) {
        if (!containsKey(key)) {
            return null;
        }
        int i = getIndexOfBucket(key, buckets.length);
        for (Node n : buckets[i]) {
            if (n.key.equals(key)) {
                return n.value;
            }
        }
        return null;
    }

    @Override
    public int size() {
        return keys.size();
    }

    @Override
    public void put(K key, V value) {
        assert(key != null);
        int i = getIndexOfBucket(key, buckets.length);
        if (!containsKey(key)) {
            buckets[i].add(createNode(key, value));
            keys.add(key);
        } else {
            for (Node n : buckets[i]) {
                if (n.key.equals(key)) {
                    n.value = value;
                }
            }
        }

        if (((double)size() / buckets.length) >= loadFactor) {
            resize(buckets.length << 1);
        }
    }

    @Override
    public Set<K> keySet() {
        return new HashSet<>(keys);
    }

    @Override
    public V remove(K key) {
        return remove(key, get(key));
    }

    @Override
    public V remove(K key, V value) {
        if (!containsKey(key)) {
            return null;
        }
        V val = get(key);
        if ((val == null && value != null) || (val != null && !val.equals(value))) {
            return null;
        }
        int i = getIndexOfBucket(key, buckets.length);
        buckets[i].remove(createNode(key, value));
        keys.remove(key);
        return val;
    }

    @Override
    public Iterator<K> iterator() {
        return keys.iterator();
    }

    private int getIndexOfBucket(K key, int tableSize) {
        return Math.floorMod(key.hashCode(), tableSize);
    }

    private void resize(int tableSize) {
        Collection<Node>[] newTable = createTable(tableSize);
        for (int i = 0; i < tableSize; i++) {
            newTable[i] = createBucket();
        }
        for (Collection<Node> bucket : buckets) {
            for (Node n : bucket) {
                int i = getIndexOfBucket(n.key, newTable.length);
                newTable[i].add(n);
            }
        }
        buckets = newTable;
    }
}
