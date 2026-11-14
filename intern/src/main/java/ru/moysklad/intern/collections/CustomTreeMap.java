package ru.moysklad.intern.collections;

import ru.moysklad.intern.collections.map.AbstractCustomMap;

public class CustomTreeMap<K, V> implements AbstractCustomMap<K, V> {
    @Override
    public V put(K key, V val) {
        return null;
    }

    @Override
    public V get(Object key) {
        return null;
    }

    @Override
    public V remove(Object key) {
        return null;
    }

    @Override
    public boolean contains(Object key) {
        return false;
    }

    @Override
    public int size() {
        return 0;
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public void clear() {

    }

    @Override
    public String toString() {
        return "";
    }

    @Override
    public boolean equals(Object val) {
        return true;
    }

    @Override
    public int hashCode() {
        return 0;
    }
}
