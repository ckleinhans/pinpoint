package edu.wisc.ece.pinpoint.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

/**
 * Class that allows for O(1) searching, adding, and removing (like HashSet), but also supports an
 * indexed ordering of elements based on the insertion order.
 */
public class OrderedHashSet<E> {
    private final HashMap<E, Integer> hashMap;
    private final ArrayList<E> list;

    public OrderedHashSet() {
        hashMap = new HashMap<>();
        list = new ArrayList<>();
    }

    /**
     * Add the specified element at the end of the list if it doesn't already exist in O(1).
     *
     * @param element to be added
     * @return true if the element was added, false if the element was already present
     */
    public boolean add(E element) {
        if (hashMap.containsKey(element)) {
            return false;
        }
        hashMap.put(element, list.size());
        list.add(element);
        return true;
    }

    /**
     * Removes the specified item in O(1).
     *
     * @param element to be removed
     * @return true if the item was removed, false if it wasn't present
     */
    public boolean remove(E element) {
        Integer listIndex = hashMap.remove(element);
        if (listIndex == null) {
            return false;
        }
        list.remove((int) listIndex);
        return true;
    }

    /**
     * Gets the item at the specified index in O(1).
     *
     * @param index of the item to get
     * @return the item
     */
    public E get(int index) {
        return list.get(index);
    }

    /**
     * Checks if the specified item is present in O(1).
     *
     * @param element to check
     * @return true if present, false if not
     */
    public boolean contains(E element) {
        return hashMap.containsKey(element);
    }

    /**
     * @return true if there are no elements, false otherwise
     */
    public boolean isEmpty() {
        return hashMap.isEmpty();
    }

    /**
     * @return the number of elements
     */
    public int size() {
        return hashMap.size();
    }

    /**
     * Clears all elements.
     */
    public void clear() {
        hashMap.clear();
        list.clear();
    }
    
    public Iterator<E> getIterator() {
        return list.iterator();
    }
}
