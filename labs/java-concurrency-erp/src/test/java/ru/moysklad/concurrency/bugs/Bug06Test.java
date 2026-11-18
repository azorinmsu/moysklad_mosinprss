package ru.moysklad.concurrency.bugs;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import ru.moysklad.concurrency.inventory.StockLedger;
import ru.moysklad.concurrency.reports.ReportService;
import ru.moysklad.concurrency.support.ConcurrencyTestProbe;
import ru.moysklad.concurrency.support.ConcurrentTasks;

import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@Timeout(10)
class Bug06Test {

    @Test
    @Tag("bug")
    void parallelTurnoverUpdatesKeepTheTotal() throws Exception {
        ConcurrencyTestProbe probe = ConcurrencyTestProbe.rendezvous(2, 800, TimeUnit.MILLISECONDS);
        ReportService service = new ReportService(new StockLedger(), probe);
        CyclicBarrier start = new CyclicBarrier(2);
        ExecutorService pool = ConcurrentTasks.pool(2, "turnover");
        try {
            pool.submit(() -> record(service, start));
            pool.submit(() -> record(service, start));
            pool.shutdown();
            assertThat(pool.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
            assertThat(service.turnoverTotal()).isEqualTo(20);
            assertThat(service.turnoverOf("main")).isEqualTo(20);
        } finally {
            ConcurrentTasks.shutdown(pool);
        }
    }

    @Test
    @Tag("baseline")
    void sequentialTurnoverAddsUp() {
        ReportService service = new ReportService(new StockLedger());
        service.recordTurnover("main", 10);
        service.recordTurnover("main", 5);
        service.recordTurnover("secondary", 7);
        assertThat(service.turnoverTotal()).isEqualTo(22);
        assertThat(service.turnoverOf("main")).isEqualTo(15);
        assertThat(service.turnoverOf("secondary")).isEqualTo(7);
    }

    private static void record(ReportService service, CyclicBarrier start) {
        try {
            start.await(2, TimeUnit.SECONDS);
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
        service.recordTurnover("main", 10);
    }
}
