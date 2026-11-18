package ru.moysklad.concurrency.bugs;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import ru.moysklad.concurrency.domain.StockMovement;
import ru.moysklad.concurrency.inventory.StockLedger;
import ru.moysklad.concurrency.reports.ReportService;
import ru.moysklad.concurrency.support.ConcurrencyTestProbe;
import ru.moysklad.concurrency.support.ConcurrentTasks;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@Timeout(10)
class Bug09Test {

    @Test
    @Tag("bug")
    void turnoverStaysConsistentWhenAMovementArrivesMidReport() throws Exception {
        ConcurrencyTestProbe probe = new ConcurrencyTestProbe();
        ReportService service = new ReportService(new StockLedger(), probe);
        service.addMovement(movement("m1", 5));
        service.addMovement(movement("m2", 7));
        ExecutorService pool = ConcurrentTasks.pool(1, "turnover-scan");
        try {
            Future<Long> sum = pool.submit(service::movementTurnover);
            assertThat(probe.awaitSignal("movement-visible", 2, TimeUnit.SECONDS)).isTrue();
            service.addMovement(movement("m3", 3));
            probe.signal("movement-mutated");
            long value = sum.get(2, TimeUnit.SECONDS);
            assertThat(value).isIn(12L, 15L);
        } catch (ExecutionException exception) {
            throw new AssertionError("Расчёт оборота не должен падать из-за параллельного движения", exception);
        } finally {
            ConcurrentTasks.shutdown(pool);
        }
    }

    @Test
    @Tag("baseline")
    void turnoverSumsRecordedMovements() {
        ReportService service = new ReportService(new StockLedger());
        service.addMovement(movement("m1", 5));
        service.addMovement(movement("m2", -2));
        assertThat(service.movementTurnover()).isEqualTo(3);
    }

    private static StockMovement movement(String id, int quantity) {
        return new StockMovement(id, "paper-a4", "main", quantity, "sale");
    }
}
