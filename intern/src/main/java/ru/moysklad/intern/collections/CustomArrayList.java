package ru.moysklad.intern.collections;

import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;

import ru.moysklad.intern.collections.exceptions.BelowZeroCapacityOfElementsException;
import ru.moysklad.intern.collections.list.AbstractCustomArrayList;

public class CustomArrayList<T> implements AbstractCustomArrayList<T> {
    private int size = 0;              // кол-во элементов в массиве
    private int capacity;              // физическая емкость массива
    private static final int DEFAULT_CAPACITY = 10;
    private Object[] elements;

    public CustomArrayList() {
        this.capacity = DEFAULT_CAPACITY;
        this.elements = new Object[DEFAULT_CAPACITY];
    }

    public CustomArrayList(int initCap) {
        if (initCap < 0) {
            throw new BelowZeroCapacityOfElementsException("Массив не может иметь отрицательное количество элементов");
        }
        this.capacity = initCap;
        this.elements = new Object[initCap];
    }

    @SafeVarargs
    public CustomArrayList(T... vals) {
        if (vals == null) {
            throw new NullPointerException("Переданный массив не может быть null");
        }
        this.elements = Arrays.copyOf(vals, vals.length, Object[].class);
        this.size = vals.length;
        this.capacity = vals.length;
    }

    // O(1) амортизированно
    @Override
    public void add(T val) {
        if (size == capacity) {
            expand();
        }
        elements[size] = val;
        size++;
    }

    // O(n) со сдвигом вправо
    // поменял реализацию с arraycopy
    @Override
    public void add(int idx, T val) {
        checkIdxForAdd(idx);

        if (size == capacity) {
            expand();
        }

        // идем с конца списка и сдвигаем элементы вправо
        System.arraycopy(elements, idx, elements, idx + 1, size - idx);

        elements[idx] = val;
        size++;
    }

    // O(1)
    @SuppressWarnings("unchecked")
    @Override
    public T get(int idx) {
        checkIdx(idx);
        return (T) elements[idx];
    }

    // O(1)
    @SuppressWarnings("unchecked")
    @Override
    public T set(int idx, T val) {
        checkIdx(idx);
        T oldVal = (T) elements[idx];
        elements[idx] = val;
        return oldVal;
    }

    // O(n) со сдвигом влево и обнулением ячейки
    @SuppressWarnings("unchecked")
    @Override
    public T remove(int idx) {
        checkIdx(idx);

        T removed = (T) elements[idx];

        for (int i = idx; i < size - 1; i++) {
            elements[i] = elements[i + 1];
        }

        // обнуляем ячейку, чтобы GC очистил объект
        elements[size - 1] = null;
        size--;

        return removed;
    }

    // O(n)
    @Override
    public boolean contains(Object val) {
        for (int i = 0; i < size; i++) {
            if (Objects.equals(elements[i], val)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int size() {
        return this.size;
    }

    @Override
    public int capacity() {
        return this.capacity;
    }

    @Override
    public boolean isEmpty() {
        return size == 0;
    }

    // Очистка массива: обнуляем ссылки для GC
    @Override
    public void clear() {
        for (int i = 0; i < size; i++) {
            elements[i] = null;
        }
        size = 0;
    }

    // Увеличение ёмкости минимум в 1.5 раза
    private void expand() {
        int oldCap = capacity;
        if (oldCap == Integer.MAX_VALUE - 8) {
            // также исправил на -8
            throw new OutOfMemoryError("Превышен максимальный размер массива: 2^31-1");
        }

        int newCap;
        if (oldCap == 0) {
            newCap = DEFAULT_CAPACITY;
        } else {
            newCap = oldCap + (oldCap >> 1);
            if (newCap <= oldCap) {
                newCap = oldCap + 1; // если oldCap == 1
            }
        }

        if (newCap < 0 || newCap > Integer.MAX_VALUE - 8) {
            newCap = (oldCap >= Integer.MAX_VALUE - 8) ? Integer.MAX_VALUE : Integer.MAX_VALUE - 8;
        }

        elements = Arrays.copyOf(elements, newCap);
        capacity = elements.length;
    }

    // Проверка границ для get, set, remove: [0, size - 1]
    private void checkIdx(int idx) {
        if (idx < 0 || idx >= size) {
            throw new IndexOutOfBoundsException(
                String.format("Индекс: %d выходит за границы массива. Размер: %d", idx, size)
            );
        }
    }

    // Проверка границ для add: [0, size]
    private void checkIdxForAdd(int idx) {
        if (idx < 0 || idx > size) {
            throw new IndexOutOfBoundsException(
                String.format("Индекс для вставки: %d выходит за границы массива. Размер: %d", idx, size)
            );
        }
    }

    @Override
    public String toString() {
        return Arrays.stream(elements, 0, size())
                     .map(String::valueOf)
                     .collect(Collectors.joining(", ", "[", "]"));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CustomArrayList<?> that = (CustomArrayList<?>) o;
        if (this.size != that.size) return false;

        for (int i = 0; i < size; i++) {
            if (!Objects.equals(this.elements[i], that.elements[i])) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        int result = 1;
        for (int i = 0; i < size; i++) {
            Object element = elements[i];
            result = 31 * result + (element == null ? 0 : element.hashCode());
        }
        return result;
    }
}
