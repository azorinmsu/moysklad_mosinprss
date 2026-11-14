package ru.moysklad.intern.collections.queue;

public interface AbstractQueue<T> {

    boolean offer(T val);

    T peek();

    T poll();

    // размер
    int size();

    // пустой?
    boolean isEmpty();

    // очистить
    void clear();
}
