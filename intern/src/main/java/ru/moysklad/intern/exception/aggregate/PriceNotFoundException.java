package ru.moysklad.intern.exception.aggregate;

import java.util.UUID;

/**
 * Бизнес-исключение: для остатка отсутствует цена (unitPrice).
 */
public class PriceNotFoundException extends RuntimeException {

    private final UUID stockId;

    public PriceNotFoundException(UUID stockId) {
        super("Цена не найдена для остатка stockId=" + stockId);
        this.stockId = stockId;
    }

    public PriceNotFoundException(String message) {
        super(message);
        this.stockId = null;
    }

    public UUID getStockId() {
        return stockId;
    }
}
