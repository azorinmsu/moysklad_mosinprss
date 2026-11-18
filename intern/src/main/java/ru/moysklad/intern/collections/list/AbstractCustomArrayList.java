package ru.moysklad.intern.collections.list;

public interface AbstractCustomArrayList<T> {

    // Добавление в конец массива
    void add(T val);

    // Добавление по индексу (вставка внутрь, со смещением)
    void add(int idx, T val);

    // Получить по индексу
    T get(int idx);

    // Установить новое значение
    T set(int idx, T val);

    // Удалить элемент массива по индексу
    T remove(int idx);

    // Проверить наличие элемента
    boolean contains(Object val);

    // Текущий размер (кол-во элементов)
    int size();

    // Физическая ёмкость массива
    int capacity();

    // Пустой ли список
    boolean isEmpty();

    // Очистить список
    void clear();
}
