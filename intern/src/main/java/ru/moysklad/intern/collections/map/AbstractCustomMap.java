package ru.moysklad.intern.collections.map;

public interface AbstractCustomMap<K, V> {

    V put(K key, V val);

    V get(Object key);

    V remove(Object key);

    boolean containsKey(Object key);

    default boolean contains(Object key) {
        return containsKey(key);
    }

    int size();

    boolean isEmpty();

    void clear();
}
