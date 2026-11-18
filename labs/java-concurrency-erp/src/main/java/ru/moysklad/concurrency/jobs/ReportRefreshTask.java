package ru.moysklad.concurrency.jobs;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Фоновый пересчёт отчёта по остаткам.
 * После {@link #requestStop()} цикл завершается.
 */
public final class ReportRefreshTask implements Runnable {

    private boolean stopRequested;
    private final CountDownLatch started = new CountDownLatch(1);
    private long cycles;

    @Override
    public void run() {
        started.countDown();
        long local = 0;
        while (!stopRequested) {
            local++;
        }
        cycles = local;
    }

    public void requestStop() {
        stopRequested = true;
    }

    public boolean awaitStarted(long timeout, TimeUnit unit) throws InterruptedException {
        return started.await(timeout, unit);
    }

    public long cycles() {
        return cycles;
    }
}
