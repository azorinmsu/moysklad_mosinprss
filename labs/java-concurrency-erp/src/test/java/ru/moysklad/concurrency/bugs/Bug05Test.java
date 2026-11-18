package ru.moysklad.concurrency.bugs;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import ru.moysklad.concurrency.domain.Shipment;
import ru.moysklad.concurrency.inventory.StockLedger;
import ru.moysklad.concurrency.shipment.ShipmentService;
import ru.moysklad.concurrency.support.ConcurrentTasks;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Timeout(10)
class Bug05Test {

    @Test
    @Tag("bug")
    void shipmentAfterInvalidAttemptStillRuns() throws Exception {
        StockLedger ledger = new StockLedger();
        ledger.put("paper-a4", "main", 5);
        ShipmentService service = new ShipmentService(ledger);
        assertThatThrownBy(() -> service.ship("SO-empty", "paper-a4", "main", 0))
                .isInstanceOf(IllegalArgumentException.class);
        ExecutorService pool = ConcurrentTasks.pool(1, "ship-retry");
        try {
            Future<Shipment> next = pool.submit(() -> service.ship("SO-2", "paper-a4", "main", 1));
            assertThatCode(() -> next.get(1, TimeUnit.SECONDS)).doesNotThrowAnyException();
            assertThat(ledger.require("paper-a4", "main").getQuantity()).isEqualTo(4);
        } finally {
            ConcurrentTasks.shutdown(pool);
        }
    }

    @Test
    @Tag("baseline")
    void twoValidShipmentsWriteOffSequentially() {
        StockLedger ledger = new StockLedger();
        ledger.put("paper-a4", "main", 5);
        ShipmentService service = new ShipmentService(ledger);
        service.ship("SO-1", "paper-a4", "main", 2);
        service.ship("SO-2", "paper-a4", "main", 1);
        assertThat(ledger.require("paper-a4", "main").getQuantity()).isEqualTo(2);
    }
}
