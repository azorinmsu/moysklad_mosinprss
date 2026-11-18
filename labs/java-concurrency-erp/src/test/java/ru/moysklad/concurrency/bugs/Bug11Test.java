package ru.moysklad.concurrency.bugs;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import ru.moysklad.concurrency.jobs.ExternalSyncGateway;
import ru.moysklad.concurrency.support.ConcurrentTasks;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@Timeout(10)
class Bug11Test {

    @Test
    @Tag("bug")
    void rejectedSyncDoesNotRaiseTheParallelismLimit() throws Exception {
        ExternalSyncGateway gateway = new ExternalSyncGateway(1);
        CountDownLatch inside = new CountDownLatch(1);
        CountDownLatch hold = new CountDownLatch(1);
        AtomicInteger maxInFlight = new AtomicInteger();
        AtomicInteger inFlight = new AtomicInteger();
        ExecutorService pool = ConcurrentTasks.pool(3, "sync");
        try {
            Future<Boolean> holder = pool.submit(() -> gateway.execute(() -> {
                int now = inFlight.incrementAndGet();
                maxInFlight.accumulateAndGet(now, Math::max);
                inside.countDown();
                await(hold);
                inFlight.decrementAndGet();
            }));
            assertThat(inside.await(2, TimeUnit.SECONDS)).isTrue();
            Future<Boolean> rejected = pool.submit(() -> gateway.execute(() -> {
                int now = inFlight.incrementAndGet();
                maxInFlight.accumulateAndGet(now, Math::max);
                inFlight.decrementAndGet();
            }));
            assertThat(rejected.get(2, TimeUnit.SECONDS)).isFalse();
            CountDownLatch thirdEntered = new CountDownLatch(1);
            Future<Boolean> third = pool.submit(() -> gateway.execute(() -> {
                int now = inFlight.incrementAndGet();
                maxInFlight.accumulateAndGet(now, Math::max);
                thirdEntered.countDown();
                await(hold);
                inFlight.decrementAndGet();
            }));
            boolean overlapped = thirdEntered.await(500, TimeUnit.MILLISECONDS);
            hold.countDown();
            assertThat(holder.get(2, TimeUnit.SECONDS)).isTrue();
            third.get(2, TimeUnit.SECONDS);
            assertThat(overlapped).isFalse();
            assertThat(maxInFlight.get()).isLessThanOrEqualTo(1);
            assertThat(gateway.availablePermits()).isEqualTo(1);
        } finally {
            hold.countDown();
            ConcurrentTasks.shutdown(pool);
        }
    }

    @Test
    @Tag("baseline")
    void successfulCallReturnsThePermit() {
        ExternalSyncGateway gateway = new ExternalSyncGateway(2);
        assertThat(gateway.execute(() -> {
        })).isTrue();
        assertThat(gateway.availablePermits()).isEqualTo(2);
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await(3, TimeUnit.SECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }
}
