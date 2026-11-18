package ru.moysklad.concurrency.support;

import java.util.concurrent.TimeUnit;

/**
 * Точки управления расписанием потоков для детерминированных тестов.
 * В рабочем режиме используется {@link #NOOP}: вызовы ничего не блокируют.
 * Тесты подставляют реализацию, которая собирает потоки в одной точке.
 */
public interface SyncPoint {

    SyncPoint NOOP = new SyncPoint() {
    };

    /**
     * Ждёт, пока заданное число потоков дойдёт до этой точки, но не дольше
     * таймаута реализации. Если соседи не пришли, поток продолжает работу.
     */
    default void rendezvous() {
    }

    default void signal(String point) {
    }

    default boolean awaitSignal(String point, long timeout, TimeUnit unit) {
        return true;
    }
}
