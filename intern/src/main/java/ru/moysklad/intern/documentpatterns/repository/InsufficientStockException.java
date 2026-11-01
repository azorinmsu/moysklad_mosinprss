package ru.moysklad.intern.documentpatterns.repository;

import ru.moysklad.intern.documentpatterns.model.DocumentErrorCode;
import ru.moysklad.intern.documentpatterns.model.DocumentProcessingException;

import java.math.BigDecimal;

/**
 * Применение набора движений привело бы к отрицательному остатку. Ни одно движение набора не применено.
 */
public class InsufficientStockException extends DocumentProcessingException {

    private final StockKey key;
    private final BigDecimal available;
    private final BigDecimal required;

    public InsufficientStockException(StockKey key, BigDecimal available, BigDecimal required) {
        super(DocumentErrorCode.INSUFFICIENT_STOCK,
                "Недостаточно товара " + key.productId() + " на складе " + key.warehouseId()
                        + ": доступно " + available.toPlainString() + ", требуется " + required.toPlainString());
        this.key = key;
        this.available = available;
        this.required = required;
    }

    public StockKey getKey() {
        return key;
    }

    public BigDecimal getAvailable() {
        return available;
    }

    public BigDecimal getRequired() {
        return required;
    }

    public BigDecimal getMissing() {
        return required.subtract(available);
    }
}
