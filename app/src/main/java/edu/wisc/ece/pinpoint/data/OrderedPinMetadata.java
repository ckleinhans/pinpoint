package edu.wisc.ece.pinpoint.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

/**
 * Class that allows for O(1) searching, adding, and removing (like HashSet), but also supports an
 * indexed ordering of elements based on the insertion order.
 */
public class OrderedPinMetadata {
    private final HashMap<PinMetadata, Integer> hashMap;
    private final ArrayList<PinMetadata> list;

    public OrderedPinMetadata() {
        hashMap = new HashMap<>();
        list = new ArrayList<>();
    }

    /**
     * Add the specified element at the start of the list if it doesn't already exist in O(1).
     *
     * @param element to be added
     * @return true if the element was added, false if the element was already present
     */
    public boolean add(PinMetadata element) {
        if (hashMap.containsKey(element)) {
            return false;
        }
        hashMap.put(element, list.size());
        list.add(element);
        return true;
    }

    /**
     * Removes the specified item in O(n).
     *
     * @param element to be removed
     * @return true if the item was removed, false if it wasn't present
     */
    public boolean remove(PinMetadata element) {
        Integer listIndex = hashMap.remove(element);
        if (listIndex == null) {
            return false;
        }
        list.remove((int) listIndex);
        return true;
    }

    /**
     * Removes the specified item with provided ID in O(n).
     *
     * @param pinId to be removed
     * @return true if the item was removed, false if it wasn't present
     */
    public boolean remove(String pinId) {
        Integer listIndex = hashMap.remove(new PinMetadata(pinId, null, null, null));
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
    public PinMetadata get(int index) {
        return list.get(list.size() - 1 - index);
    }

    /**
     * Checks if the specified item is present in O(1).
     *
     * @param element to check
     * @return true if present, false if not
     */
    public boolean contains(PinMetadata element) {
        return hashMap.containsKey(element);
    }

    /**
     * Checks if the specified item is present in O(1).
     *
     * @param pinId to check
     * @return true if present, false if not
     */
    public boolean contains(String pinId) {
        return hashMap.containsKey(new PinMetadata(pinId, null, null, null));
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

    public Iterator<PinMetadata> getIterator() {
        return list.iterator();
    }
}
