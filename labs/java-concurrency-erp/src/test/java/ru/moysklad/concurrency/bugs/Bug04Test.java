package ru.moysklad.concurrency.bugs;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import ru.moysklad.concurrency.domain.InsufficientStockException;
import ru.moysklad.concurrency.inventory.StockLedger;
import ru.moysklad.concurrency.support.ConcurrencyTestProbe;
import ru.moysklad.concurrency.support.ConcurrentTasks;
import ru.moysklad.concurrency.transfer.StockTransferService;

import java.lang.management.ManagementFactory;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

@Timeout(15)
class Bug04Test {

    @Test
    @Tag("bug")
    void oppositeTransfersFinishAndConserveStock() throws Exception {
        StockLedger ledger = new StockLedger();
        ledger.put("paper-a4", "A", 10);
        ledger.put("paper-a4", "B", 10);
        ConcurrencyTestProbe probe = new ConcurrencyTestProbe();
        StockTransferService service = new StockTransferService(ledger, probe);
        CyclicBarrier start = new CyclicBarrier(2);
        ExecutorService pool = ConcurrentTasks.pool(2, "transfer");
        try {
            Future<?> toB = pool.submit(() -> {
                start.await(2, TimeUnit.SECONDS);
                return service.transfer("paper-a4", "A", "B", 4);
            });
            Future<?> toA = pool.submit(() -> {
                start.await(2, TimeUnit.SECONDS);
                return service.transfer("paper-a4", "B", "A", 4);
            });
            boolean firstA = probe.awaitSignal("first:A", 2, TimeUnit.SECONDS);
            boolean firstB = probe.awaitSignal("first:B", 2, TimeUnit.SECONDS);
            if (firstA && firstB) {
                probe.signal("second-lock");
            }
            try {
                toB.get(4, TimeUnit.SECONDS);
                toA.get(4, TimeUnit.SECONDS);
            } catch (TimeoutException exception) {
                long[] deadlocked = ManagementFactory.getThreadMXBean().findDeadlockedThreads();
                fail("Перемещения не завершились. deadlockedThreads="
                        + (deadlocked == null ? 0 : deadlocked.length));
            }
            assertThat(ledger.require("paper-a4", "A").getQuantity()).isEqualTo(10);
            assertThat(ledger.require("paper-a4", "B").getQuantity()).isEqualTo(10);
        } finally {
            ConcurrentTasks.shutdown(pool);
        }
    }

    @Test
    @Tag("baseline")
    void transferMovesQuantityAndRejectsShortage() {
        StockLedger ledger = new StockLedger();
        ledger.put("paper-a4", "A", 10);
        ledger.put("paper-a4", "B", 1);
        StockTransferService service = new StockTransferService(ledger);
        service.transfer("paper-a4", "A", "B", 4);
        assertThat(ledger.require("paper-a4", "A").getQuantity()).isEqualTo(6);
        assertThat(ledger.require("paper-a4", "B").getQuantity()).isEqualTo(5);
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.transfer("paper-a4", "A", "B", 100))
                .isInstanceOf(InsufficientStockException.class);
        assertThat(ledger.require("paper-a4", "A").getQuantity()).isEqualTo(6);
        assertThat(ledger.require("paper-a4", "B").getQuantity()).isEqualTo(5);
    }
}
