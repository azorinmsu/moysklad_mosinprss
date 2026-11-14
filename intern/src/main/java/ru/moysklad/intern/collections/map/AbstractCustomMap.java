package ru.moysklad.intern.collections.map;

public interface AbstractCustomMap<K, V> {

    // вставка
    V put(K key, V val);

    // получить по ключу
    V get(Object key);

    // удалить по ключу
    V remove(Object key);

    // содержит (по ключу);
    boolean contains(Object key);

    // размер
    int size();

    // пустой?
    boolean isEmpty();

    // очистить
    void clear();

}
