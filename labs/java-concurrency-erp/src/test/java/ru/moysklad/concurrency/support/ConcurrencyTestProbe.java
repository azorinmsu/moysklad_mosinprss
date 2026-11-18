package ru.moysklad.concurrency.support;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Управляемая точка синхронизации тестов.
 * {@link #rendezvous()} собирает заданное число потоков. Если за отведённое время
 * они не собрались (например, взаимное исключение не пускает второй поток),
 * ожидание заканчивается и поток идёт дальше.
 */
public final class ConcurrencyTestProbe implements SyncPoint {

    private final int parties;
    private final long waitMillis;
    private final AtomicInteger arrived = new AtomicInteger();
    private final CountDownLatch gate = new CountDownLatch(1);
    private final ConcurrentHashMap<String, CountDownLatch> signals = new ConcurrentHashMap<>();

    public ConcurrencyTestProbe() {
        this(1, 0);
    }

    public static ConcurrencyTestProbe rendezvous(int parties, long wait, TimeUnit unit) {
        if (parties < 1) {
            throw new IllegalArgumentException("parties");
        }
        return new ConcurrencyTestProbe(parties, unit.toMillis(wait));
    }

    private ConcurrencyTestProbe(int parties, long waitMillis) {
        this.parties = parties;
        this.waitMillis = waitMillis;
    }

    @Override
    public void rendezvous() {
        if (parties <= 1) {
            return;
        }
        if (arrived.incrementAndGet() >= parties) {
            gate.countDown();
            return;
        }
        try {
            gate.await(waitMillis, TimeUnit.MILLISECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void signal(String point) {
        latch(point).countDown();
    }

    @Override
    public boolean awaitSignal(String point, long timeout, TimeUnit unit) {
        try {
            return latch(point).await(timeout, unit);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private CountDownLatch latch(String point) {
        return signals.computeIfAbsent(point, ignored -> new CountDownLatch(1));
    }
}
