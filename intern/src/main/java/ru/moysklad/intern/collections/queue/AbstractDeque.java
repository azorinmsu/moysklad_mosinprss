package ru.moysklad.intern.collections.queue;

public interface AbstractDeque<T> {

    void addFirst(T val);

    void addLast(T val);

    T removeFirst();

    T removeLast();

    T peekFirst();

    T peekLast();

    // размер
    int size();

    // пустой?
    boolean isEmpty();

    // очистить
    void clear();
}
