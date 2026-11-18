package ru.moysklad.concurrency.synchronizers;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import ru.moysklad.concurrency.support.ConcurrentTasks;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Tag("synchronizer")
@Timeout(10)
class CustomCyclicBarrierTest {

    @Test
    void partiesLeaveOnlyAfterTheLastArrival() throws Exception {
        CustomCyclicBarrier barrier = new CustomCyclicBarrier(3);
        AtomicInteger arrived = new AtomicInteger();
        List<Integer> indexes = new java.util.concurrent.CopyOnWriteArrayList<>();
        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            Thread thread = daemon(() -> {
                arrived.incrementAndGet();
                try {
                    indexes.add(barrier.await());
                } catch (InterruptedException | BrokenBarrierException exception) {
                    throw new IllegalStateException(exception);
                }
                if (arrived.get() != 3) {
                    throw new IllegalStateException("барьер открылся рано");
                }
            }, "party-" + i);
            threads.add(thread);
            thread.start();
        }
        for (Thread thread : threads) {
            thread.join(2_000);
            assertThat(thread.isAlive()).isFalse();
        }
        assertThat(indexes).containsExactlyInAnyOrder(0, 1, 2);
        assertThat(barrier.getNumberWaiting()).isZero();
    }

    @Test
    void barrierCanBeReused() throws Exception {
        CustomCyclicBarrier barrier = new CustomCyclicBarrier(2);
        AtomicInteger phase = new AtomicInteger();
        Runnable party = () -> {
            try {
                barrier.await();
                phase.incrementAndGet();
                barrier.await();
                phase.incrementAndGet();
            } catch (InterruptedException | BrokenBarrierException exception) {
                throw new IllegalStateException(exception);
            }
        };
        Thread first = daemon(party, "phase-1");
        Thread second = daemon(party, "phase-2");
        first.start();
        second.start();
        first.join(2_000);
        second.join(2_000);
        assertThat(first.isAlive()).isFalse();
        assertThat(second.isAlive()).isFalse();
        assertThat(phase).hasValue(4);
    }

    @Test
    void interruptBreaksTheBarrierForTheOthers() throws Exception {
        CustomCyclicBarrier barrier = new CustomCyclicBarrier(3);
        AtomicInteger broken = new AtomicInteger();
        Thread waiter = daemon(() -> {
            try {
                barrier.await();
            } catch (BrokenBarrierException exception) {
                broken.incrementAndGet();
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            }
        }, "barrier-waiter");
        waiter.start();
        ConcurrentTasks.awaitState(waiter, Thread.State.WAITING, 2_000);
        assertThat(barrier.getNumberWaiting()).isEqualTo(1);
        Thread interrupter = daemon(() -> {
            try {
                barrier.await();
            } catch (InterruptedException exception) {
                broken.addAndGet(10);
            } catch (BrokenBarrierException exception) {
                broken.addAndGet(100);
            }
        }, "barrier-interrupt");
        interrupter.start();
        ConcurrentTasks.awaitState(interrupter, Thread.State.WAITING, 2_000);
        interrupter.interrupt();
        interrupter.join(2_000);
        waiter.join(2_000);
        assertThat(waiter.isAlive()).isFalse();
        assertThat(broken.get() % 10).isEqualTo(1);
        assertThat(broken.get() / 10).isEqualTo(1);
        assertThatThrownBy(barrier::await).isInstanceOf(BrokenBarrierException.class);
    }

    @Test
    void timeoutBreaksTheBarrierUntilReset() throws Exception {
        CustomCyclicBarrier barrier = new CustomCyclicBarrier(3);
        Thread waiter = daemon(() -> {
            try {
                barrier.await();
            } catch (BrokenBarrierException | InterruptedException exception) {
                // поколение сломано таймаутом второго участника
            }
        }, "barrier-timeout-waiter");
        waiter.start();
        ConcurrentTasks.awaitState(waiter, Thread.State.WAITING, 2_000);
        assertThatThrownBy(() -> barrier.await(150, TimeUnit.MILLISECONDS))
                .isInstanceOf(TimeoutException.class);
        waiter.join(2_000);
        assertThat(waiter.isAlive()).isFalse();
        assertThatThrownBy(barrier::await).isInstanceOf(BrokenBarrierException.class);
        barrier.reset();
        Thread first = daemon(() -> awaitQuietly(barrier), "reset-1");
        Thread second = daemon(() -> awaitQuietly(barrier), "reset-2");
        Thread third = daemon(() -> awaitQuietly(barrier), "reset-3");
        first.start();
        second.start();
        third.start();
        first.join(2_000);
        second.join(2_000);
        third.join(2_000);
        assertThat(first.isAlive()).isFalse();
        assertThat(second.isAlive()).isFalse();
        assertThat(third.isAlive()).isFalse();
    }

    @Test
    void resetBreaksWaitersAndStartsANewGeneration() throws Exception {
        CustomCyclicBarrier barrier = new CustomCyclicBarrier(2);
        java.util.concurrent.atomic.AtomicBoolean broken = new java.util.concurrent.atomic.AtomicBoolean();
        Thread waiter = daemon(() -> {
            try {
                barrier.await();
            } catch (BrokenBarrierException exception) {
                broken.set(true);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            }
        }, "barrier-reset");
        waiter.start();
        ConcurrentTasks.awaitState(waiter, Thread.State.WAITING, 2_000);
        barrier.reset();
        waiter.join(2_000);
        assertThat(broken).isTrue();
        assertThat(barrier.getNumberWaiting()).isZero();
    }

    @Test
    void negativePartiesAreRejected() {
        assertThatThrownBy(() -> new CustomCyclicBarrier(0)).isInstanceOf(IllegalArgumentException.class);
    }

    private static void awaitQuietly(CustomCyclicBarrier barrier) {
        try {
            barrier.await();
        } catch (InterruptedException | BrokenBarrierException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static Thread daemon(Runnable runnable, String name) {
        Thread thread = new Thread(runnable, name);
        thread.setDaemon(true);
        return thread;
    }
}
