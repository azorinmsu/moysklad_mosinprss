package ru.moysklad.intern.documentpatterns.support;

import ru.moysklad.intern.documentpatterns.repository.InMemoryStockRepository;
import ru.moysklad.intern.entity.Product;
import ru.moysklad.intern.entity.warehouse.Warehouse;

import java.math.BigDecimal;

/**
 * Подготовка остатков для тестов.
 */
public final class TestStocks {

    private TestStocks() {
    }

    public static InMemoryStockRepository empty() {
        return new InMemoryStockRepository();
    }

    public static InMemoryStockRepository withQuantity(Product product, Warehouse warehouse, String quantity) {
        return new InMemoryStockRepository().setQuantity(product, warehouse, new BigDecimal(quantity));
    }

    /**
     * Начальные остатки сквозного сценария:
     * <pre>
     * A: X = 20, Y = 10
     * B: X = 0,  Y = 0
     * </pre>
     */
    public static InMemoryStockRepository workflowInitialStocks(Product x, Product y, Warehouse a, Warehouse b) {
        return new InMemoryStockRepository()
                .setQuantity(x, a, new BigDecimal("20"))
                .setQuantity(y, a, new BigDecimal("10"))
                .setQuantity(x, b, BigDecimal.ZERO)
                .setQuantity(y, b, BigDecimal.ZERO);
    }
}
