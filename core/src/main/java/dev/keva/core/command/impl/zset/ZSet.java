package dev.keva.core.command.impl.zset;

import dev.keva.util.hashbytes.BytesKey;
import lombok.NonNull;

import java.io.Serializable;
import java.util.AbstractMap.SimpleEntry;
import java.util.AbstractSet;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.NavigableSet;
import java.util.TreeSet;

/**
 * A SortedSet implementation tailor-made for Redis, and hence no generic definition.
 * The current implementation uses TreeSet which internally used Balanced BST.
 * In the future, if needed, we can implement a SkipList.
 */
public class ZSet extends AbstractSet<SimpleEntry<Double, BytesKey>> implements NavigableSet<SimpleEntry<Double, BytesKey>>, Serializable {

    private static final long serialVersionUID = 1L;

    private final HashMap<BytesKey, Double> keys = new HashMap<>();

    private final TreeSet<SimpleEntry<Double, BytesKey>> scores = new TreeSet<>((Comparator<SimpleEntry<Double, BytesKey>> & Serializable)(e1, e2) -> {
        int cmp = e1.getKey().compareTo(e2.getKey());
        if (cmp != 0) {
            return cmp;
        }
        return e1.getValue().compareTo(e2.getValue());
    });

    @Override
    public SimpleEntry<Double, BytesKey> lower(SimpleEntry<Double, BytesKey> entry) {
        return scores.lower(entry);
    }

    @Override
    public SimpleEntry<Double, BytesKey> floor(SimpleEntry<Double, BytesKey> entry) {
        return scores.floor(entry);
    }

    @Override
    public SimpleEntry<Double, BytesKey> ceiling(SimpleEntry<Double, BytesKey> entry) {
        return scores.ceiling(entry);
    }

    @Override
    public SimpleEntry<Double, BytesKey> higher(SimpleEntry<Double, BytesKey> entry) {
        return scores.higher(entry);
    }

    @Override
    public SimpleEntry<Double, BytesKey> pollFirst() {
        return scores.pollFirst();
    }

    @Override
    public SimpleEntry<Double, BytesKey> pollLast() {
        return scores.pollLast();
    }

    @Override
    public int size() {
        return keys.size();
    }

    @Override
    public boolean isEmpty() {
        return keys.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return scores.contains(o);
    }

    @Override
    public Iterator<SimpleEntry<Double, BytesKey>> iterator() {
        return scores.iterator();
    }

    @Override
    public Object[] toArray() {
        return scores.toArray();
    }

    @Override
    public <T> T[] toArray(@NonNull T[] ts) {
        return scores.toArray(ts);
    }

    @Override
    public boolean add(SimpleEntry<Double, BytesKey> entry) {
        boolean result = true;
        if (keys.containsKey(entry.getValue())){
            result = false;
            scores.remove(new SimpleEntry<>(keys.get(entry.getValue()), entry.getValue()));
        }
        scores.add(new SimpleEntry<>(entry.getKey(), entry.getValue()));
        keys.put(entry.getValue(), entry.getKey());
        return result;
    }

    @Override
    public boolean remove(@NonNull Object o) {
        SimpleEntry<Double, BytesKey> entry = (SimpleEntry<Double, BytesKey>) o;
        if (keys.containsKey(entry.getValue())){
            scores.remove(new SimpleEntry<>(entry.getKey(), entry.getValue()));
            keys.remove(entry.getValue());
            return true;
        }
        return false;
    }

    public boolean removeByKey(@NonNull BytesKey key) {
        if (keys.containsKey(key)) {
            scores.remove(new SimpleEntry<>(keys.get(key), key));
            keys.remove(key);
            return true;
        }
        return false;
    }

    @Override
    public synchronized void clear() {
        scores.clear();
        keys.clear();
    }

    @NonNull
    @Override
    public NavigableSet<SimpleEntry<Double, BytesKey>> descendingSet() {
        return scores.descendingSet();
    }

    @NonNull
    @Override
    public Iterator<SimpleEntry<Double, BytesKey>> descendingIterator() {
        return scores.descendingIterator();
    }

    @NonNull
    @Override
    public NavigableSet<SimpleEntry<Double, BytesKey>> subSet(SimpleEntry<Double, BytesKey> start, boolean b1, SimpleEntry<Double, BytesKey> end, boolean b2) {
        return scores.subSet(start, b1, end, b2);
    }

    @NonNull
    @Override
    public NavigableSet<SimpleEntry<Double, BytesKey>> headSet(SimpleEntry<Double, BytesKey> entry, boolean b) {
        return scores.headSet(entry, b);
    }

    @NonNull
    @Override
    public NavigableSet<SimpleEntry<Double, BytesKey>> tailSet(SimpleEntry<Double, BytesKey> entry, boolean b) {
        return scores.tailSet(entry, b);
    }

    @Override
    public Comparator<? super SimpleEntry<Double, BytesKey>> comparator() {
        return scores.comparator();
    }

    @NonNull
    @Override
    public java.util.SortedSet<SimpleEntry<Double, BytesKey>> subSet(SimpleEntry<Double, BytesKey> begin, SimpleEntry<Double, BytesKey> end) {
        return scores.subSet(begin, end);
    }

    @NonNull
    @Override
    public java.util.SortedSet<SimpleEntry<Double, BytesKey>> headSet(SimpleEntry<Double, BytesKey> entry) {
        return scores.headSet(entry);
    }

    @NonNull
    @Override
    public java.util.SortedSet<SimpleEntry<Double, BytesKey>> tailSet(SimpleEntry<Double, BytesKey> entry) {
        return scores.tailSet(entry);
    }

    @Override
    public SimpleEntry<Double, BytesKey> first() {
        return scores.first();
    }

    @Override
    public SimpleEntry<Double, BytesKey> last() {
        return scores.last();
    }

    public Double getScore(BytesKey key) {
        return keys.get(key);
    }

    @Override
    public String toString() {
        return keys.toString();
    }
}
