package ru.moysklad.concurrency.inventory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.moysklad.concurrency.domain.InsufficientStockException;
import ru.moysklad.concurrency.domain.StockBalance;
import ru.moysklad.concurrency.support.SyncPoint;

import java.util.HashMap;
import java.util.Map;

@Service
public class InventoryService {

    private final StockLedger ledger;
    private final SyncPoint syncPoint;
    private final Map<String, Integer> balanceCache = new HashMap<>();

    @Autowired
    public InventoryService(StockLedger ledger) {
        this(ledger, SyncPoint.NOOP);
    }

    public InventoryService(StockLedger ledger, SyncPoint syncPoint) {
        this.ledger = ledger;
        this.syncPoint = syncPoint;
    }

    public StockBalance acceptOpeningBalance(String productId, String warehouseId, int quantity) {
        if (quantity < 0) {
            throw new IllegalArgumentException("Начальный остаток не может быть отрицательным");
        }
        return ledger.put(productId, warehouseId, quantity);
    }

    /**
     * Увеличивает остаток на величину прихода.
     */
    public StockBalance receive(String productId, String warehouseId, int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Приход должен быть положительным");
        }
        StockBalance stock = ledger.require(productId, warehouseId);
        stock.setQuantity(stock.getQuantity() + quantity);
        return stock;
    }

    /**
     * Списывает товар со склада.
     * Остаток не становится отрицательным: при нехватке выбрасывается {@link InsufficientStockException}.
     * Итоговое количество равно начальному минус сумма успешных списаний.
     */
    public void writeOff(String productId, String warehouseId, int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Списание должно быть положительным");
        }
        Object lock = new Object();
        synchronized (lock) {
            StockBalance stock = ledger.require(productId, warehouseId);
            int current = stock.getQuantity();
            syncPoint.rendezvous();
            if (current < quantity) {
                throw new InsufficientStockException(productId, warehouseId, quantity, current);
            }
            stock.setQuantity(current - quantity);
        }
    }

    /**
     * Кэш остатка для быстрых карточек товара.
     * Повторные увеличения одного ключа суммируются.
     */
    public void addCachedBalance(String productId, int delta) {
        if (delta <= 0) {
            throw new IllegalArgumentException("Изменение кэша должно быть положительным");
        }
        Integer current = balanceCache.get(productId);
        int base = current == null ? 0 : current;
        syncPoint.rendezvous();
        balanceCache.put(productId, base + delta);
    }

    public int cachedBalance(String productId) {
        Integer value = balanceCache.get(productId);
        return value == null ? 0 : value;
    }
}
