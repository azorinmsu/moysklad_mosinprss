package ru.moysklad.concurrency.bugs;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import ru.moysklad.concurrency.domain.InsufficientStockException;
import ru.moysklad.concurrency.domain.StockBalance;
import ru.moysklad.concurrency.inventory.InventoryService;
import ru.moysklad.concurrency.inventory.StockLedger;
import ru.moysklad.concurrency.support.ConcurrencyTestProbe;
import ru.moysklad.concurrency.support.ConcurrentTasks;

import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@Timeout(10)
class Bug03Test {

    @Test
    @Tag("bug")
    void parallelWriteOffKeepsStockInvariant() throws Exception {
        StockLedger ledger = new StockLedger();
        StockBalance stock = ledger.put("pen-blue", "main", 5);
        ConcurrencyTestProbe probe = ConcurrencyTestProbe.rendezvous(2, 800, TimeUnit.MILLISECONDS);
        InventoryService service = new InventoryService(ledger, probe);
        CyclicBarrier start = new CyclicBarrier(2);
        ExecutorService pool = ConcurrentTasks.pool(2, "write-off");
        try {
            Future<?> first = pool.submit(() -> writeOff(service, start));
            Future<?> second = pool.submit(() -> writeOff(service, start));
            int successes = countSuccesses(first, second);
            assertThat(successes).isEqualTo(1);
            assertThat(stock.getQuantity()).isEqualTo(2);
            assertThat(stock.getQuantity()).isGreaterThanOrEqualTo(0);
        } finally {
            ConcurrentTasks.shutdown(pool);
        }
    }

    @Test
    @Tag("baseline")
    void sequentialWriteOffRejectsTheSecondWhenStockIsShort() {
        StockLedger ledger = new StockLedger();
        ledger.put("pen-blue", "main", 5);
        InventoryService service = new InventoryService(ledger);
        service.writeOff("pen-blue", "main", 3);
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.writeOff("pen-blue", "main", 3))
                .isInstanceOf(InsufficientStockException.class);
        assertThat(ledger.require("pen-blue", "main").getQuantity()).isEqualTo(2);
    }

    private static void writeOff(InventoryService service, CyclicBarrier start) {
        try {
            start.await(2, TimeUnit.SECONDS);
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
        service.writeOff("pen-blue", "main", 3);
    }

    private static int countSuccesses(Future<?> first, Future<?> second) throws Exception {
        int successes = 0;
        successes += outcome(first);
        successes += outcome(second);
        return successes;
    }

    private static int outcome(Future<?> future) throws Exception {
        try {
            future.get(5, TimeUnit.SECONDS);
            return 1;
        } catch (ExecutionException exception) {
            if (exception.getCause() instanceof InsufficientStockException) {
                return 0;
            }
            throw exception;
        }
    }
}
