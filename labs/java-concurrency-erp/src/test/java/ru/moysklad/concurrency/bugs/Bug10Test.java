package ru.moysklad.concurrency.bugs;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import ru.moysklad.concurrency.inventory.StockLedger;
import ru.moysklad.concurrency.shipment.ShipmentService;
import ru.moysklad.concurrency.support.ConcurrentTasks;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;

@Timeout(10)
class Bug10Test {

    @Test
    @Tag("bug")
    void shipmentWaitsUntilTheOrderedProductArrives() throws Exception {
        ShipmentService service = new ShipmentService(new StockLedger(), "paper-a4");
        ExecutorService pool = ConcurrentTasks.pool(1, "await-stock");
        try {
            Future<?> shipment = pool.submit(() -> {
                service.shipWhenAvailable(5);
                return null;
            });
            assertThat(service.awaitWaiterParked(2, TimeUnit.SECONDS)).isTrue();
            service.receiveGoods("pen-blue", 10);
            boolean finishedEarly;
            try {
                shipment.get(800, TimeUnit.MILLISECONDS);
                finishedEarly = true;
            } catch (TimeoutException exception) {
                finishedEarly = false;
            }
            assertThat(finishedEarly).as("отгрузка бумаги не выполняется из-за прихода другого товара").isFalse();
            service.receiveGoods("paper-a4", 5);
            shipment.get(2, TimeUnit.SECONDS);
            assertThat(service.incomingUnits()).isZero();
        } finally {
            ConcurrentTasks.shutdown(pool);
        }
    }

    @Test
    @Tag("baseline")
    void shipmentProceedsWhenStockIsAlreadyThere() throws Exception {
        ShipmentService service = new ShipmentService(new StockLedger(), "paper-a4");
        service.receiveGoods("paper-a4", 8);
        service.shipWhenAvailable(3);
        assertThat(service.incomingUnits()).isEqualTo(5);
    }
}
