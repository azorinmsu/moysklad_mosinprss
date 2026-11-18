package ru.moysklad.concurrency.inventory;

import org.springframework.stereotype.Component;
import ru.moysklad.concurrency.domain.StockBalance;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class StockLedger {

    private final ConcurrentHashMap<String, StockBalance> balances = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Object> warehouseLocks = new ConcurrentHashMap<>();

    public StockBalance put(String productId, String warehouseId, int quantity) {
        StockBalance balance = new StockBalance(productId, warehouseId, quantity);
        balances.put(key(productId, warehouseId), balance);
        return balance;
    }

    public StockBalance require(String productId, String warehouseId) {
        StockBalance balance = balances.get(key(productId, warehouseId));
        if (balance == null) {
            throw new IllegalStateException("Нет остатка " + productId + " на складе " + warehouseId);
        }
        return balance;
    }

    public List<StockBalance> snapshot() {
        List<StockBalance> copy = new ArrayList<>();
        for (StockBalance balance : balances.values()) {
            copy.add(balance.copy());
        }
        return List.copyOf(copy);
    }

    public Object warehouseLock(String warehouseId) {
        return warehouseLocks.computeIfAbsent(warehouseId, id -> new Object());
    }

    private static String key(String productId, String warehouseId) {
        return productId + "@" + warehouseId;
    }
}
