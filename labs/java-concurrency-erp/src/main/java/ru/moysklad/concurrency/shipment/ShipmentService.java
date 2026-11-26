package ru.moysklad.concurrency.shipment;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.moysklad.concurrency.domain.InsufficientStockException;
import ru.moysklad.concurrency.domain.Shipment;
import ru.moysklad.concurrency.domain.StockBalance;
import ru.moysklad.concurrency.inventory.StockLedger;

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class ShipmentService {

    private final StockLedger ledger;
    private final ReentrantLock shipmentLock = new ReentrantLock();
    private final ReentrantLock arrivalLock = new ReentrantLock();
    private final Condition stockArrived = arrivalLock.newCondition();
    private String watchedProductId = "";
    private int incomingUnits;
    private final java.util.concurrent.CountDownLatch waitingForStock = new java.util.concurrent.CountDownLatch(1);

    @Autowired
    public ShipmentService(StockLedger ledger) {
        this.ledger = ledger;
    }

    public ShipmentService(StockLedger ledger, String watchedProductId) {
        this.ledger = ledger;
        this.watchedProductId = watchedProductId;
    }

    /**
     * Отгружает зарезервированный товар.
     * После завершения вызова, в том числе при ошибке входных данных,
     * другие отгрузки могут выполняться.
     */
    public Shipment ship(String orderId, String productId, String warehouseId, int quantity) {

        if (quantity <= 0)
            throw new IllegalArgumentException("Пустая отгрузка");

        try {
            shipmentLock.lock();

            StockBalance stock = ledger.require(productId, warehouseId);

            if (stock.getQuantity() < quantity)
                throw new InsufficientStockException(productId, warehouseId, quantity, stock.getQuantity());

            stock.setQuantity(stock.getQuantity() - quantity);
            return new Shipment(
                    UUID.randomUUID().toString(),
                    orderId,
                    productId,
                    warehouseId,
                    quantity
            );

        } finally {
            shipmentLock.unlock();
        }
    }

    /**
     * Отгружает товар, когда на склад пришло не меньше {@code requested} единиц
     * наблюдаемого товара. Чужой приход не считается выполнением заказа.
     * Остаток наблюдаемого товара не уходит в минус.
     */
    public void shipWhenAvailable(int requested) throws InterruptedException {
        if (requested <= 0)
            throw new IllegalArgumentException("Количество отгрузки должно быть положительным");

        arrivalLock.lock();
        try {
            while (incomingUnits < requested) { // здесь проблема if -> while
                waitingForStock.countDown();
                stockArrived.await();
            }
            incomingUnits -= requested;
        } finally {
            arrivalLock.unlock();
        }
    }

    public void receiveGoods(String productId, int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Приход должен быть положительным");
        }
        arrivalLock.lock();
        try {
            if (productId.equals(watchedProductId)) {
                incomingUnits += quantity;
            }
            stockArrived.signalAll();
        } finally {
            arrivalLock.unlock();
        }
    }

    public int incomingUnits() {
        arrivalLock.lock();
        try {
            return incomingUnits;
        } finally {
            arrivalLock.unlock();
        }
    }

    public void watch(String productId) {
        this.watchedProductId = productId;
    }

    /**
     * Ждёт, пока поток отгрузки войдёт в ожидание и отпустит монитор.
     */
    public boolean awaitWaiterParked(long timeout, TimeUnit unit) throws InterruptedException {
        long deadline = System.nanoTime() + unit.toNanos(timeout);
        long remaining = deadline - System.nanoTime();
        if (remaining <= 0 || !waitingForStock.await(remaining, TimeUnit.NANOSECONDS)) {
            return false;
        }
        while (System.nanoTime() < deadline) {
            if (arrivalLock.tryLock()) {
                arrivalLock.unlock();
                return true;
            }
            Thread.onSpinWait();
        }
        return false;
    }
}
