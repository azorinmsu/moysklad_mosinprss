package ru.moysklad.concurrency.bugs;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import ru.moysklad.concurrency.domain.ReservationResult;
import ru.moysklad.concurrency.domain.StockBalance;
import ru.moysklad.concurrency.inventory.StockLedger;
import ru.moysklad.concurrency.reservation.ReservationService;
import ru.moysklad.concurrency.support.ConcurrencyTestProbe;
import ru.moysklad.concurrency.support.ConcurrentTasks;

import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@Timeout(10)
class Bug01Test {

    @Test
    @Tag("bug")
    void twoBuyersCannotBothReserveTheLastUnit() throws Exception {
        StockLedger ledger = new StockLedger();
        StockBalance stock = ledger.put("paper-a4", "main", 1);
        ConcurrencyTestProbe probe = ConcurrencyTestProbe.rendezvous(2, 800, TimeUnit.MILLISECONDS);
        ReservationService service = new ReservationService(ledger, probe);
        CyclicBarrier start = new CyclicBarrier(2);
        ExecutorService pool = ConcurrentTasks.pool(2, "reserve");
        try {
            Future<ReservationResult> first = pool.submit(() -> {
                start.await(2, TimeUnit.SECONDS);
                return service.reserve("SO-1", "paper-a4", "main", 1);
            });
            Future<ReservationResult> second = pool.submit(() -> {
                start.await(2, TimeUnit.SECONDS);
                return service.reserve("SO-2", "paper-a4", "main", 1);
            });
            long accepted = 0;
            if (first.get(5, TimeUnit.SECONDS).accepted()) {
                accepted++;
            }
            if (second.get(5, TimeUnit.SECONDS).accepted()) {
                accepted++;
            }
            assertThat(accepted).isEqualTo(1);
            assertThat(stock.getReserved()).isEqualTo(1);
            assertThat(stock.available()).isZero();
            assertThat(service.reservations()).hasSize(1);
        } finally {
            ConcurrentTasks.shutdown(pool);
        }
    }

    @Test
    @Tag("baseline")
    void secondSequentialReservationOfLastUnitIsRejected() {
        StockLedger ledger = new StockLedger();
        ledger.put("paper-a4", "main", 1);
        ReservationService service = new ReservationService(ledger);
        ReservationResult first = service.reserve("SO-1", "paper-a4", "main", 1);
        ReservationResult second = service.reserve("SO-2", "paper-a4", "main", 1);
        assertThat(first.accepted()).isTrue();
        assertThat(second.accepted()).isFalse();
        assertThat(ledger.require("paper-a4", "main").getReserved()).isEqualTo(1);
        assertThat(service.reservations()).hasSize(1);
    }
}
