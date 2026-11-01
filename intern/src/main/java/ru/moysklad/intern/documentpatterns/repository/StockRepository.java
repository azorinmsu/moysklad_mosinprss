package ru.moysklad.intern.documentpatterns.repository;

import ru.moysklad.intern.documentpatterns.model.StockMovement;
import ru.moysklad.intern.entity.Product;
import ru.moysklad.intern.entity.warehouse.Warehouse;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Хранилище количественных остатков лабораторной.
 *
 * <p>Интерфейс нужен, чтобы в тестах подменять хранилище (например, имитировать сбой при применении движений).
 * Учебная реализация — {@link InMemoryStockRepository}. С таблицей {@code aggregate_stock} основного
 * приложения не связан.
 */
public interface StockRepository {

    /** Текущий остаток; для неизвестного ключа — {@code 0}. */
    BigDecimal getQuantity(StockKey key);

    default BigDecimal getQuantity(Product product, Warehouse warehouse) {
        return getQuantity(StockKey.of(product, warehouse));
    }

    /**
     * Атомарно применяет набор движений: либо применены все, либо ни одно.
     *
     * @throws InsufficientStockException если по какому-либо ключу итоговый остаток стал бы отрицательным
     * @throws IllegalArgumentException   если набор или движение некорректны
     */
    void apply(List<StockMovement> movements);

    /** Журнал всех применённых движений в порядке применения. */
    List<StockHistoryEntry> history();

    default List<StockHistoryEntry> history(UUID documentId) {
        return history().stream()
                .filter(entry -> documentId.equals(entry.movement().documentId()))
                .toList();
    }
}
