package ru.moysklad.concurrency.synchronizers;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import ru.moysklad.concurrency.support.ConcurrentTasks;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Tag("synchronizer")
@Timeout(10)
class CustomSemaphoreTest {

    @Test
    void acquireAndReleaseRestorePermits() throws Exception {
        CustomSemaphore semaphore = new CustomSemaphore(2);
        semaphore.acquire();
        assertThat(semaphore.availablePermits()).isEqualTo(1);
        semaphore.release();
        assertThat(semaphore.availablePermits()).isEqualTo(2);
    }

    @Test
    void atMostTheLimitRunTogether() throws Exception {
        CustomSemaphore semaphore = new CustomSemaphore(2);
        CountDownLatch inside = new CountDownLatch(2);
        CountDownLatch hold = new CountDownLatch(1);
        ExecutorService pool = ConcurrentTasks.pool(2, "semaphore");
        try {
            for (int i = 0; i < 2; i++) {
                pool.submit(() -> {
                    semaphore.acquire();
                    inside.countDown();
                    hold.await(3, TimeUnit.SECONDS);
                    semaphore.release();
                    return null;
                });
            }
            assertThat(inside.await(2, TimeUnit.SECONDS)).isTrue();
            assertThat(semaphore.tryAcquire()).isFalse();
            assertThat(semaphore.tryAcquire(200, TimeUnit.MILLISECONDS)).isFalse();
            hold.countDown();
        } finally {
            hold.countDown();
            ConcurrentTasks.shutdown(pool);
        }
    }

    @Test
    void acquireIsInterruptibleAndDoesNotTakeAPermit() throws Exception {
        CustomSemaphore semaphore = new CustomSemaphore(0);
        AtomicBoolean interrupted = new AtomicBoolean();
        Thread waiter = new Thread(() -> {
            try {
                semaphore.acquire();
            } catch (InterruptedException exception) {
                interrupted.set(true);
            }
        }, "semaphore-interrupt");
        waiter.setDaemon(true);
        waiter.start();
        ConcurrentTasks.awaitState(waiter, Thread.State.WAITING, 2_000);
        waiter.interrupt();
        waiter.join(2_000);
        assertThat(interrupted).isTrue();
        assertThat(semaphore.availablePermits()).isZero();
    }

    @Test
    void tryAcquireTimesOutAndKeepsThePermitCount() throws Exception {
        CustomSemaphore semaphore = new CustomSemaphore(0);
        long started = System.nanoTime();
        assertThat(semaphore.tryAcquire(150, TimeUnit.MILLISECONDS)).isFalse();
        assertThat(System.nanoTime() - started).isGreaterThanOrEqualTo(TimeUnit.MILLISECONDS.toNanos(120));
        assertThat(semaphore.availablePermits()).isZero();
    }

    @Test
    void releaseWakesABlockedAcquire() throws Exception {
        CustomSemaphore semaphore = new CustomSemaphore(0);
        AtomicBoolean entered = new AtomicBoolean();
        Thread waiter = new Thread(() -> {
            try {
                semaphore.acquire();
                entered.set(true);
                semaphore.release();
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            }
        }, "semaphore-wakeup");
        waiter.setDaemon(true);
        waiter.start();
        ConcurrentTasks.awaitState(waiter, Thread.State.WAITING, 2_000);
        semaphore.release();
        waiter.join(2_000);
        assertThat(waiter.isAlive()).isFalse();
        assertThat(entered).isTrue();
        assertThat(semaphore.availablePermits()).isEqualTo(1);
    }

    @Test
    void negativePermitsAreRejectedAndTryAcquireDoesNotBlock() {
        assertThatThrownBy(() -> new CustomSemaphore(-1)).isInstanceOf(IllegalArgumentException.class);
        CustomSemaphore semaphore = new CustomSemaphore(0);
        AtomicInteger calls = new AtomicInteger();
        assertThat(semaphore.tryAcquire()).isFalse();
        assertThat(calls).hasValue(0);
        assertThat(semaphore.availablePermits()).isZero();
    }

}
