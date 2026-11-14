package ru.moysklad.intern.collections;

import ru.moysklad.intern.collections.queue.AbstractQueue;

public class CustomPriorityQueue<T extends Comparable<T>> implements AbstractQueue<T> {

    @Override
    public boolean offer(T val) {
        return false;
    }

    @Override
    public T peek() {
        return null;
    }

    @Override
    public T poll() {
        return null;
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
