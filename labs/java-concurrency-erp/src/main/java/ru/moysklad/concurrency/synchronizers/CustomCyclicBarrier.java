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

    private final int parties;
    private Generation generation = new Generation();

    private static final class Generation {
        private int count;
        private boolean broken;
    }

    public CustomCyclicBarrier(int parties) {
        if (parties <= 0) {
            throw new IllegalArgumentException("parties must be positive");
        }
        this.parties = parties;
    }

    public synchronized int await() throws InterruptedException, BrokenBarrierException {
        Generation current = generation;
        if (current.broken)
            throw new BrokenBarrierException(); // поломался барьер и потоки не продолжат работать

        int index = current.count++;
        if (index == parties - 1) {
            generation = new Generation();
            notifyAll();
            return 0;
        }

        try {
            while (generation == current && !current.broken) {
                wait();
            }
        } catch (InterruptedException exception) {
            if (generation == current) {
                current.broken = true;
                notifyAll();
            }
            throw exception;
        }

        if (current.broken)
            throw new BrokenBarrierException();
        return parties - 1 - index;
    }

    public synchronized int await(long timeout, TimeUnit unit)
            throws InterruptedException, BrokenBarrierException, TimeoutException {
        long remaining = unit.toNanos(timeout);
        Generation current = generation;
        if (current.broken)
            throw new BrokenBarrierException();

        int index = current.count++;
        if (index == parties - 1) {
            generation = new Generation();
            notifyAll();
            return 0;
        }

        try {
            while (generation == current && !current.broken) {
                if (remaining <= 0) {
                    current.broken = true;
                    notifyAll();
                    throw new TimeoutException();
                }
                long started = System.nanoTime();
                wait(remaining / 1_000_000L, (int) (remaining % 1_000_000L));
                remaining -= System.nanoTime() - started;
            }
        } catch (InterruptedException exception) {
            if (generation == current) {
                current.broken = true;
                notifyAll();
            }
            throw exception;
        }

        if (current.broken)
            throw new BrokenBarrierException();
        return parties - 1 - index;
    }

    public synchronized void reset() {
        generation.broken = true;
        generation = new Generation();
        notifyAll();
    }

    public synchronized int getNumberWaiting() {
        return generation.broken ? 0 : generation.count;
    }
}
