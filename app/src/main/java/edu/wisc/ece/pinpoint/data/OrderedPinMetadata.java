package edu.wisc.ece.pinpoint.data;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import edu.wisc.ece.pinpoint.utils.FirebaseDriver;

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

    public OrderedPinMetadata(@Nullable Map<String, Object> pinData) {
        hashMap = new HashMap<>();
        list = new ArrayList<>();
        if (pinData == null) {
            return;
        }
        // Cast to PinMetadata objects & sort based on timestamp
        ArrayList<PinMetadata> tempList = new ArrayList<>();
        for (String pinId : pinData.keySet()) {
            //noinspection unchecked,ConstantConditions
            tempList.add(new PinMetadata(pinId, (Map<String, Object>) pinData.get(pinId)));
        }
        tempList.sort(Comparator.comparing(PinMetadata::getTimestamp));

        // Insert all items into hashmap & list
        for (PinMetadata metadata : tempList) {
            if (hashMap.containsKey(metadata)) {
                throw new IllegalArgumentException("pinData cannot contain duplicate pin IDs");
            }
            hashMap.put(metadata, list.size());
            list.add(metadata);
        }
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
        // update all elements hashmap entries
        for (int i = listIndex; i < list.size(); i++) {
            hashMap.replace(list.get(i), i);
        }
        return true;
    }

    /**
     * Removes the specified item with provided ID in O(n).
     *
     * @param pinId to be removed
     * @return true if the item was removed, false if it wasn't present
     */
    public boolean remove(String pinId) {
        return remove(new PinMetadata(pinId, null, null, null, null));
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
        return contains(new PinMetadata(pinId, null, null, null, null));
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

    public OrderedPinMetadata filterBySource(PinMetadata.PinSource source) {
        OrderedPinMetadata ret = new OrderedPinMetadata();
        if (source == PinMetadata.PinSource.FRIEND) {
            FirebaseDriver firebase = FirebaseDriver.getInstance();
            this.list.forEach(p -> {
                Pin pin = firebase.getCachedPin(p.getPinId());
                // if this pin is by someone who current user is following
                if (firebase.getCachedFollowing(firebase.getUid()).contains(pin.getAuthorUID())) {
                    ret.add(p);
                }
            });
        } else if (source == PinMetadata.PinSource.GENERAL) {
            FirebaseDriver firebase = FirebaseDriver.getInstance();
            this.list.forEach(p -> {
                if (p.getPinSource() == PinMetadata.PinSource.GENERAL) {
                    Pin pin = firebase.getCachedPin(p.getPinId());
                    // if this pin is by someone who current user isn't following
                    if (!firebase.getCachedFollowing(firebase.getUid())
                            .contains(pin.getAuthorUID())) {
                        ret.add(p);
                    }
                }
            });
        } else {
            this.list.forEach(p -> {
                if (p.getPinSource() == source) {
                    ret.add(p);
                }
            });
        }
        return ret;
    }
}
