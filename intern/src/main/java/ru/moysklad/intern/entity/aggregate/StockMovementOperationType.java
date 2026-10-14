package ru.moysklad.intern.entity.aggregate;

/**
 * Тип операции, породившей запись в истории количественных остатков.
 */
public enum StockMovementOperationType {
    GOODS_ACCEPTANCE,
    SHIPMENT,
    SHIFT,
    MACHINING,
    MAKE,
    SALES
}
