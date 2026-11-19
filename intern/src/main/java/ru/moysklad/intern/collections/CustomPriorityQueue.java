package ru.moysklad.intern.collections;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Objects;
import ru.moysklad.intern.collections.queue.AbstractCustomQueue;

public class CustomPriorityQueue<T> implements AbstractCustomQueue<T> {

    private static final int DEFAULT_CAPACITY = 11;
    private Object[] queue;
    private int size = 0;
    private final Comparator<? super T> comparator;

    public CustomPriorityQueue() {
        this(DEFAULT_CAPACITY, null);
    }

    public CustomPriorityQueue(Comparator<? super T> comparator) {
        this(DEFAULT_CAPACITY, comparator);
    }

    public CustomPriorityQueue(int initialCapacity) {
        this(initialCapacity, null);
    }

    public CustomPriorityQueue(int initialCapacity, Comparator<? super T> comparator) {
        if (initialCapacity < 1) {
            throw new IllegalArgumentException("Начальная ёмкость должна быть >= 1");
        }
        this.queue = new Object[initialCapacity];
        this.comparator = comparator;
    }

    @SuppressWarnings("unchecked")
    private int compare(T a, T b) {
        if (comparator != null) {
            return comparator.compare(a, b);
        }
        return ((Comparable<? super T>) a).compareTo(b);
    }

    // Вставка элемента с просеиванием вверх: O(log n)
    @Override
    public boolean offer(T val) {
        if (val == null) {
            throw new NullPointerException("Null-элементы запрещены в PriorityQueue");
        }
        if (size >= queue.length) {
            grow();
        }
        siftUp(size, val);
        size++;
        return true;
    }

    // Просмотр минимального элемента (вершина кучи): O(1)
    @SuppressWarnings("unchecked")
    @Override
    public T peek() {
        return size == 0 ? null : (T) queue[0];
    }

    // Извлечение минимального элемента с просеиванием вниз: O(log n)
    @SuppressWarnings("unchecked")
    @Override
    public T poll() {
        if (size == 0) {
            return null;
        }
        int s = --size;
        T result = (T) queue[0];
        T x = (T) queue[s];
        queue[s] = null; // обнуляем ячейку для GC

        if (s != 0) {
            siftDown(0, x);
        }
        return result;
    }

    // Просеивание вверх: сравниваем с родителем (k - 1) / 2
    @SuppressWarnings("unchecked")
    private void siftUp(int k, T x) {
        while (k > 0) {
            int parent = (k - 1) >>> 1;
            Object e = queue[parent];
            if (compare(x, (T) e) >= 0) {
                break;
            }
            queue[k] = e;
            k = parent;
        }
        queue[k] = x;
    }

    // Просеивание вниз: сравниваем с наименьшим из детей 2*k + 1 и 2*k + 2
    @SuppressWarnings("unchecked")
    private void siftDown(int k, T x) {
        int half = size >>> 1;
        while (k < half) {
            int child = (k << 1) + 1; // левый ребенок: 2*k + 1
            Object c = queue[child];
            int right = child + 1;    // правый ребенок: 2*k + 2

            if (right < size && compare((T) c, (T) queue[right]) > 0) {
                child = right;
                c = queue[child];
            }

            if (compare(x, (T) c) <= 0) {
                break;
            }
            queue[k] = c;
            k = child;
        }
        queue[k] = x;
    }

    private void grow() {
        int oldCap = queue.length;
        int newCap = oldCap + (oldCap < 64 ? (oldCap + 2) : (oldCap >> 1));
        queue = Arrays.copyOf(queue, newCap);
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
            queue[i] = null;
        }
        size = 0;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < size; i++) {
            if (i > 0) sb.append(", ");
            sb.append(queue[i]);
        }
        return sb.append("]").toString();
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CustomPriorityQueue<?> that = (CustomPriorityQueue<?>) o;
        if (this.size != that.size) return false;


        // скопируем сами массивы, чтобы не нарушить исходники
        Object[] a1 = Arrays.copyOf(this.queue, this.size);
        Object[] a2 = Arrays.copyOf(that.queue, that.size);

        // отсортируем компаратором
        Arrays.sort(a1, (x, y) -> this.compare((T) x, (T) y));
        Arrays.sort(a2, (x, y) -> this.compare((T) x, (T) y));

        return Arrays.equals(a1, a2);
    }

    @Override
    public int hashCode() {
        // тк порядок не детерминирован в куче, то посчитаем порядконезависимый хэш
        // (сумма всех хэшей элементов)
        int result = 0;
        for (int i = 0; i < size; i++)
            result += Objects.hashCode(queue[i]);

        return result;
    }
}
