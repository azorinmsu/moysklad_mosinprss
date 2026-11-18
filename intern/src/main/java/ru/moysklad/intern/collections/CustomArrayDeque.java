package ru.moysklad.intern.collections;

import java.util.NoSuchElementException;
import java.util.Objects;
import ru.moysklad.intern.collections.queue.AbstractCustomDeque;

public class CustomArrayDeque<T> implements AbstractCustomDeque<T> {

    private static final int DEFAULT_CAPACITY = 16;
    private Object[] elements;
    private int head = 0;
    private int size = 0;

    public CustomArrayDeque() {
        this(DEFAULT_CAPACITY);
    }

    public CustomArrayDeque(int initialCapacity) {
        if (initialCapacity < 1) {
            throw new IllegalArgumentException("Начальная ёмкость должна быть >= 1");
        }
        this.elements = new Object[initialCapacity];
    }

    // Добавление в начало: O(1) амортизированно
    @Override
    public void addFirst(T val) {
        if (val == null) {
            throw new NullPointerException("Null-элементы запрещены в Deque");
        }
        if (size == elements.length) {
            expand();
        }
        head = (head - 1 + elements.length) % elements.length;
        elements[head] = val;
        size++;
    }

    // Добавление в конец: O(1) амортизированно
    @Override
    public void addLast(T val) {
        if (val == null) {
            throw new NullPointerException("Null-элементы запрещены в Deque");
        }
        if (size == elements.length) {
            expand();
        }
        int tail = (head + size) % elements.length;
        elements[tail] = val;
        size++;
    }

    // Удаление из начала: O(1)
    @SuppressWarnings("unchecked")
    @Override
    public T removeFirst() {
        if (size == 0) {
            throw new NoSuchElementException("Очередь пуста");
        }
        T val = (T) elements[head];
        elements[head] = null; // обнуление ячейки для GC
        head = (head + 1) % elements.length;
        size--;
        return val;
    }

    // Удаление с конца: O(1)
    @SuppressWarnings("unchecked")
    @Override
    public T removeLast() {
        if (size == 0) {
            throw new NoSuchElementException("Очередь пуста");
        }
        int lastIdx = (head + size - 1) % elements.length;
        T val = (T) elements[lastIdx];
        elements[lastIdx] = null; // обнуление ячейки для GC
        size--;
        return val;
    }

    // Просмотр начала: O(1)
    @SuppressWarnings("unchecked")
    @Override
    public T peekFirst() {
        return size == 0 ? null : (T) elements[head];
    }

    // Просмотр конца: O(1)
    @SuppressWarnings("unchecked")
    @Override
    public T peekLast() {
        if (size == 0) {
            return null;
        }
        int lastIdx = (head + size - 1) % elements.length;
        return (T) elements[lastIdx];
    }

    // Расширение кольцевого массива с сохранением логического порядка
    private void expand() {
        int oldCap = elements.length;
        int newCap = oldCap * 2;
        Object[] newElements = new Object[newCap];

        for (int i = 0; i < size; i++) {
            newElements[i] = elements[(head + i) % oldCap];
        }

        this.elements = newElements;
        this.head = 0;
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
        for (int i = 0; i < size; i++) {
            elements[(head + i) % elements.length] = null;
        }
        head = 0;
        size = 0;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < size; i++) {
            if (i > 0) sb.append(", ");
            sb.append(elements[(head + i) % elements.length]);
        }
        return sb.append("]").toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CustomArrayDeque<?> that = (CustomArrayDeque<?>) o;
        if (this.size != that.size) return false;
        for (int i = 0; i < size; i++) {
            Object thisElem = this.elements[(this.head + i) % this.elements.length];
            Object thatElem = that.elements[(that.head + i) % that.elements.length];
            if (!Objects.equals(thisElem, thatElem)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        int result = 1;
        for (int i = 0; i < size; i++) {
            Object elem = elements[(head + i) % elements.length];
            result = 31 * result + (elem == null ? 0 : elem.hashCode());
        }
        return result;
    }
}
