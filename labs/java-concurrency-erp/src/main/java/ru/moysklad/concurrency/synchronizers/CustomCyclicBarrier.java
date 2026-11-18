package ru.moysklad.concurrency.synchronizers;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Циклический барьер этапов расчёта отчёта по оборотам.
 *
 * <p>Контракт:
 * <ul>
 *   <li>{@code parties} потоков должны вызвать {@link #await()} ;
 *       ни один не проходит барьер, пока не пришли все;</li>
 *   <li>после полного набора барьер открывается и сразу готов к следующему поколению;</li>
 *   <li>{@link #await()} возвращает {@code parties - 1 - индексПрибытия}, последний поток возвращает 0;</li>
 *   <li>прерывание или таймаут ломают текущее поколение:
 *       прерванный поток получает {@link InterruptedException},
 *       опоздавший — {@link TimeoutException},
 *       остальные ожидания этого поколения — {@link BrokenBarrierException};</li>
 *   <li>{@link #reset()} ломает ожидающих и открывает новое исправное поколение;</li>
 *   <li>{@link #getNumberWaiting()} возвращает число потоков, стоящих на барьере.</li>
 * </ul>
 *
 * <p>Не делегируйте работу {@link java.util.concurrent.CyclicBarrier}.
 */
public class CustomCyclicBarrier {

    public CustomCyclicBarrier(int parties) {
        if (parties <= 0) {
            throw new IllegalArgumentException("parties must be positive");
        }
    }

    public int await() throws InterruptedException, BrokenBarrierException {
        throw new UnsupportedOperationException("SYNC-04");
    }

    public int await(long timeout, TimeUnit unit)
            throws InterruptedException, BrokenBarrierException, TimeoutException {
        throw new UnsupportedOperationException("SYNC-04");
    }

    public void reset() {
        throw new UnsupportedOperationException("SYNC-04");
    }

    public int getNumberWaiting() {
        throw new UnsupportedOperationException("SYNC-04");
    }
}
