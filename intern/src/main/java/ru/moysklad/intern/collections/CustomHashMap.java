package ru.moysklad.intern.collections;

import java.util.Objects;
import ru.moysklad.intern.collections.map.AbstractCustomMap;

public class CustomHashMap<K, V> implements AbstractCustomMap<K, V> {

    // Узел односвязного списка для разрешения коллизий
    private static class Entry<K, V> {
        final K key;
        V value;
        final int hash;
        Entry<K, V> next;

        Entry(int hash, K key, V value, Entry<K, V> next) {
            this.hash = hash;
            this.key = key;
            this.value = value;
            this.next = next;
        }
    }

    private static final int DEFAULT_CAPACITY = 16;
    private static final float LOAD_FACTOR = 0.75f;

    @SuppressWarnings("unchecked")
    private Entry<K, V>[] table = (Entry<K, V>[]) new Entry[DEFAULT_CAPACITY];
    private int size = 0;
    private int threshold = (int) (DEFAULT_CAPACITY * LOAD_FACTOR);

    public CustomHashMap() {
    }

    // Хеш ключа с обработкой null и перемешиванием старших битов
    private int hash(Object key) {
        if (key == null) {
            return 0;
        }
        int h = key.hashCode();
        return h ^ (h >>> 16);
    }

    // Индекс корзины с защитой от отрицательных значений
    private int getIndex(int hash, int capacity) {
        return (hash & 0x7FFFFFFF) % capacity;
    }

    @Override
    public V put(K key, V val) {
        int hash = hash(key);
        int idx = getIndex(hash, table.length);

        // Поиск существующего ключа
        for (Entry<K, V> node = table[idx]; node != null; node = node.next) {
            if (node.hash == hash && Objects.equals(node.key, key)) {
                V oldVal = node.value;
                node.value = val;
                return oldVal;
            }
        }

        // Вставка в голову цепочки
        table[idx] = new Entry<>(hash, key, val, table[idx]);
        size++;

        if (size > threshold) {
            resize();
        }

        return null;
    }

    @Override
    public V get(Object key) {
        int hash = hash(key);
        int idx = getIndex(hash, table.length);

        for (Entry<K, V> node = table[idx]; node != null; node = node.next) {
            if (node.hash == hash && Objects.equals(node.key, key)) {
                return node.value;
            }
        }
        return null;
    }

    @Override
    public V remove(Object key) {
        int hash = hash(key);
        int idx = getIndex(hash, table.length);

        Entry<K, V> prev = null;
        Entry<K, V> curr = table[idx];

        while (curr != null) {
            if (curr.hash == hash && Objects.equals(curr.key, key)) {
                if (prev == null) {
                    table[idx] = curr.next;
                } else {
                    prev.next = curr.next;
                }
                size--;
                return curr.value;
            }
            prev = curr;
            curr = curr.next;
        }

        return null;
    }

    @Override
    public boolean containsKey(Object key) {
        int hash = hash(key);
        int idx = getIndex(hash, table.length);

        for (Entry<K, V> node = table[idx]; node != null; node = node.next) {
            if (node.hash == hash && Objects.equals(node.key, key)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public boolean isEmpty() {
        return size == 0;
    }

    @Override
    public void clear() {
        for (int i = 0; i < table.length; i++) {
            table[i] = null;
        }
        size = 0;
    }

    // Увеличение массива корзин в 2 раза и перераспределение элементов
    @SuppressWarnings("unchecked")
    private void resize() {
        int newCap = table.length * 2;
        Entry<K, V>[] newTable = (Entry<K, V>[]) new Entry[newCap];

        for (Entry<K, V> kvEntry : table) {
            Entry<K, V> curr = kvEntry;
            while (curr != null) {
                Entry<K, V> next = curr.next;
                int newIdx = getIndex(curr.hash, newCap);

                curr.next = newTable[newIdx];
                newTable[newIdx] = curr;

                curr = next;
            }
        }

        this.table = newTable;
        this.threshold = (int) (newCap * LOAD_FACTOR);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Entry<K, V> head : table) {
            for (Entry<K, V> node = head; node != null; node = node.next) {
                if (!first) sb.append(", ");
                sb.append(node.key).append("=").append(node.value);
                first = false;
            }
        }
        return sb.append("}").toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CustomHashMap<?, ?> that = (CustomHashMap<?, ?>) o;
        if (this.size != that.size) return false;

        for (Entry<K, V> head : table) {
            for (Entry<K, V> node = head; node != null; node = node.next) {
                Object thatVal = that.get(node.key);
                if (thatVal == null) {
                    if (node.value != null || !that.containsKey(node.key)) {
                        return false;
                    }
                } else if (!thatVal.equals(node.value)) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        int h = 0;
        for (Entry<K, V> head : table) {
            for (Entry<K, V> node = head; node != null; node = node.next) {
                h += Objects.hashCode(node.key) ^ Objects.hashCode(node.value);
            }
        }
        return h;
    }
}
