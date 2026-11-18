package ru.moysklad.concurrency.bugs;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import ru.moysklad.concurrency.inventory.StockLedger;
import ru.moysklad.concurrency.orders.SalesOrderService;
import ru.moysklad.concurrency.reservation.ReservationService;
import ru.moysklad.concurrency.support.ConcurrencyTestProbe;
import ru.moysklad.concurrency.support.ConcurrentTasks;

import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@Timeout(10)
class Bug08Test {

    @Test
    @Tag("bug")
    void parallelOrdersAreAllCounted() throws Exception {
        ConcurrencyTestProbe probe = ConcurrencyTestProbe.rendezvous(2, 800, TimeUnit.MILLISECONDS);
        SalesOrderService service = new SalesOrderService(
                new ReservationService(new StockLedger()),
                probe
        );
        CyclicBarrier start = new CyclicBarrier(2);
        ExecutorService pool = ConcurrentTasks.pool(2, "orders");
        try {
            pool.submit(() -> count(service, start));
            pool.submit(() -> count(service, start));
            pool.shutdown();
            assertThat(pool.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
            assertThat(service.orderCount("customer-1")).isEqualTo(2);
        } finally {
            ConcurrentTasks.shutdown(pool);
        }
    }

    @Test
    @Tag("baseline")
    void sequentialOrdersIncrementTheCounter() {
        SalesOrderService service = new SalesOrderService(new ReservationService(new StockLedger()));
        service.registerOrder("customer-1");
        service.registerOrder("customer-1");
        service.registerOrder("customer-2");
        assertThat(service.orderCount("customer-1")).isEqualTo(2);
        assertThat(service.orderCount("customer-2")).isEqualTo(1);
    }

    private static void count(SalesOrderService service, CyclicBarrier start) {
        try {
            start.await(2, TimeUnit.SECONDS);
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
        service.registerOrder("customer-1");
    }
}
