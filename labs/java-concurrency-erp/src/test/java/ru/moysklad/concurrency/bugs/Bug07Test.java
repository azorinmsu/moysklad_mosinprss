package ru.moysklad.concurrency.bugs;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import ru.moysklad.concurrency.inventory.InventoryService;
import ru.moysklad.concurrency.inventory.StockLedger;
import ru.moysklad.concurrency.support.ConcurrencyTestProbe;
import ru.moysklad.concurrency.support.ConcurrentTasks;

import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@Timeout(10)
class Bug07Test {

    @Test
    @Tag("bug")
    void parallelCacheIncrementsKeepEveryUnit() throws Exception {
        ConcurrencyTestProbe probe = ConcurrencyTestProbe.rendezvous(2, 800, TimeUnit.MILLISECONDS);
        InventoryService service = new InventoryService(new StockLedger(), probe);
        CyclicBarrier start = new CyclicBarrier(2);
        ExecutorService pool = ConcurrentTasks.pool(2, "cache");
        try {
            pool.submit(() -> add(service, start));
            pool.submit(() -> add(service, start));
            pool.shutdown();
            assertThat(pool.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
            assertThat(service.cachedBalance("paper-a4")).isEqualTo(6);
        } finally {
            ConcurrentTasks.shutdown(pool);
        }
    }

    @Test
    @Tag("baseline")
    void sequentialCacheIncrementsAddUp() {
        InventoryService service = new InventoryService(new StockLedger());
        service.addCachedBalance("paper-a4", 3);
        service.addCachedBalance("paper-a4", 3);
        assertThat(service.cachedBalance("paper-a4")).isEqualTo(6);
        assertThat(service.cachedBalance("missing")).isZero();
    }

    private static void add(InventoryService service, CyclicBarrier start) {
        try {
            start.await(2, TimeUnit.SECONDS);
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
        service.addCachedBalance("paper-a4", 3);
    }
}
