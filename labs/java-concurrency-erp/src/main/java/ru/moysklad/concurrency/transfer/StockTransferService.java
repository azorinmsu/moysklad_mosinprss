package ru.moysklad.concurrency.transfer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.moysklad.concurrency.domain.InsufficientStockException;
import ru.moysklad.concurrency.domain.StockBalance;
import ru.moysklad.concurrency.domain.StockTransfer;
import ru.moysklad.concurrency.inventory.StockLedger;
import ru.moysklad.concurrency.support.SyncPoint;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class StockTransferService {

    private final StockLedger ledger;
    private final SyncPoint syncPoint;

    @Autowired
    public StockTransferService(StockLedger ledger) {
        this(ledger, SyncPoint.NOOP);
    }

    public StockTransferService(StockLedger ledger, SyncPoint syncPoint) {
        this.ledger = ledger;
        this.syncPoint = syncPoint;
    }

    /**
     * Перемещает товар между складами.
     * Сумма остатков по складам не меняется.
     * Встречные перемещения завершаются, остатки не уходят в минус.
     */
    public StockTransfer transfer(String productId, String fromWarehouseId, String toWarehouseId, int quantity) {
        if (quantity <= 0)
            throw new IllegalArgumentException("Количество перемещения должно быть положительным");

        if (fromWarehouseId.equals(toWarehouseId))
            throw new IllegalArgumentException("Склады отправления и назначения должны различаться");

        boolean fromIsFirst = fromWarehouseId.compareTo(toWarehouseId) < 0;

        Object firstLock = ledger.warehouseLock(fromIsFirst ? fromWarehouseId : toWarehouseId);
        Object secondLock = ledger.warehouseLock(fromIsFirst ? toWarehouseId : fromWarehouseId);

        synchronized (firstLock) {
            syncPoint.signal("first:" + fromWarehouseId);
            syncPoint.awaitSignal("second-lock", 500, TimeUnit.MILLISECONDS);

            synchronized (secondLock) {
                StockBalance from = ledger.require(productId, fromWarehouseId);
                StockBalance to = ledger.require(productId, toWarehouseId);

                if (from.getQuantity() < quantity) {
                    throw new InsufficientStockException(
                            productId, fromWarehouseId, quantity, from.getQuantity());
                }

                from.setQuantity(from.getQuantity() - quantity);
                to.setQuantity(to.getQuantity() + quantity);

                return new StockTransfer(
                        UUID.randomUUID().toString(),
                        productId,
                        fromWarehouseId,
                        toWarehouseId,
                        quantity
                );
            }
        }
    }
}
