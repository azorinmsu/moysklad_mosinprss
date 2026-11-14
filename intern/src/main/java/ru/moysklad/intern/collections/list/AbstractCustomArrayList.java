package ru.moysklad.intern.collections.list;

public interface AbstractCustomArrayList<T> {

    // Добавление в конец массива
    void add(T val);

    // добавление по индексу (вставка внутрь, со смещением)
    void add(int idx, T val);

    // получить по индексу Object
    T get(int idx);

    // установить новое значение
    T set(int idx, T val);

    // удалить элемент массива
    T remove(int idx);

    // есть ли элемент
    boolean contains(Object val);

    // размер
    int size();

    // пустой?
    boolean isEmpty();

    // очистить
    void clear();
}
