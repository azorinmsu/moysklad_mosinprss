package ru.moysklad.concurrency.synchronizers;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import ru.moysklad.concurrency.support.ConcurrentTasks;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Tag("synchronizer")
@Timeout(10)
class CustomCountDownLatchTest {

    @Test
    void awaitReleasesEveryWaiterWhenCountReachesZero() throws Exception {
        CustomCountDownLatch latch = new CustomCountDownLatch(1);
        ExecutorService pool = ConcurrentTasks.pool(3, "latch");
        try {
            List<Future<String>> waiters = new ArrayList<>();
            for (int i = 0; i < 3; i++) {
                waiters.add(pool.submit(() -> {
                    latch.await();
                    return "released";
                }));
            }
            latch.countDown();
            for (Future<String> waiter : waiters) {
                assertThat(waiter.get(2, TimeUnit.SECONDS)).isEqualTo("released");
            }
            assertThat(latch.getCount()).isZero();
        } finally {
            ConcurrentTasks.shutdown(pool);
        }
    }

    @Test
    void concurrentCountDownEndsAtZero() throws Exception {
        CustomCountDownLatch latch = new CustomCountDownLatch(8);
        ExecutorService pool = ConcurrentTasks.pool(8, "latch-down");
        try {
            List<Future<?>> tasks = new ArrayList<>();
            for (int i = 0; i < 8; i++) {
                tasks.add(pool.submit(latch::countDown));
            }
            for (Future<?> task : tasks) {
                task.get(2, TimeUnit.SECONDS);
            }
            assertThat(latch.getCount()).isZero();
            Future<?> late = pool.submit(() -> {
                latch.await();
                return null;
            });
            late.get(1, TimeUnit.SECONDS);
        } finally {
            ConcurrentTasks.shutdown(pool);
        }
    }

    @Test
    void awaitIsInterruptible() throws Exception {
        CustomCountDownLatch latch = new CustomCountDownLatch(1);
        AtomicBoolean interrupted = new AtomicBoolean();
        Thread waiter = new Thread(() -> {
            try {
                latch.await();
            } catch (InterruptedException exception) {
                interrupted.set(true);
            }
        }, "latch-interrupt");
        waiter.setDaemon(true);
        waiter.start();
        ConcurrentTasks.awaitState(waiter, Thread.State.WAITING, 2_000);
        waiter.interrupt();
        waiter.join(2_000);
        assertThat(waiter.isAlive()).isFalse();
        assertThat(interrupted).isTrue();
    }

    @Test
    void awaitTimesOutWithoutConsumingTheCount() throws Exception {
        CustomCountDownLatch latch = new CustomCountDownLatch(1);
        long started = System.nanoTime();
        boolean released = latch.await(200, TimeUnit.MILLISECONDS);
        long elapsed = System.nanoTime() - started;
        assertThat(released).isFalse();
        assertThat(elapsed).isGreaterThanOrEqualTo(TimeUnit.MILLISECONDS.toNanos(150));
        assertThat(latch.getCount()).isEqualTo(1);
    }

    @Test
    void extraCountDownDoesNotGoNegativeAndZeroLatchDoesNotBlock() throws Exception {
        CustomCountDownLatch latch = new CustomCountDownLatch(1);
        latch.countDown();
        latch.countDown();
        assertThat(latch.getCount()).isZero();
        CustomCountDownLatch open = new CustomCountDownLatch(0);
        AtomicInteger passed = new AtomicInteger();
        Thread thread = new Thread(() -> {
            try {
                open.await();
                passed.incrementAndGet();
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            }
        });
        thread.start();
        thread.join(1_000);
        assertThat(passed).hasValue(1);
        assertThat(open.getCount()).isZero();
    }

    @Test
    void negativeCountIsRejected() {
        assertThatThrownBy(() -> new CustomCountDownLatch(-1))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
