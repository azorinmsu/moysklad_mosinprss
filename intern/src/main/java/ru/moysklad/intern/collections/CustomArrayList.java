package ru.moysklad.intern.collections;

import ru.moysklad.intern.collections.list.AbstractCustomArrayList;

public class CustomArrayList<T extends Comparable<T>> implements AbstractCustomArrayList<T> {
    protected static int size;
    protected static double capacity;

    @Override
    public void add(T val) {

    }

    @Override
    public void add(int idx, T val) {

    }

    @Override
    public T get(int idx) {
        return null;
    }

    @Override
    public T set(int idx, T val) {
        return null;
    }

    @Override
    public T remove(int idx) {
        return null;
    }

    @Override
    public boolean contains(Object val) {
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
