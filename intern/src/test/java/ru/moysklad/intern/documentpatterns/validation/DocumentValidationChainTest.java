package ru.moysklad.intern.documentpatterns.validation;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * Часть 3. Chain of Responsibility — проверки документа.
 * Проверяйте не только тип исключения, но и {@code DocumentValidationException#getCode()}.
 */
class DocumentValidationChainTest {

    @Test
    @Disabled("TODO: implement test")
    @DisplayName("Пустой документ → EMPTY_DOCUMENT")
    void emptyDocumentIsRejected() {
        // TODO: defaultChain().validate(acceptance(a), context) → код EMPTY_DOCUMENT.
        fail("TODO: implement test");
    }

    @Test
    @Disabled("TODO: implement test")
    @DisplayName("Нулевое и отрицательное количество → INVALID_QUANTITY")
    void invalidQuantityIsRejected() {
        // TODO: position(x, "0") и position(x, "-1") → код INVALID_QUANTITY.
        fail("TODO: implement test");
    }

    @Test
    @Disabled("TODO: implement test")
    @DisplayName("Отсутствующий склад → MISSING_TARGET_WAREHOUSE / MISSING_SOURCE_WAREHOUSE")
    void missingWarehouseIsRejected() {
        // TODO: acceptance(null, ...) → MISSING_TARGET_WAREHOUSE; shipment(null, ...) → MISSING_SOURCE_WAREHOUSE;
        //  transfer(a, null, ...) → MISSING_TARGET_WAREHOUSE.
        fail("TODO: implement test");
    }

    @Test
    @Disabled("TODO: implement test")
    @DisplayName("Перемещение на тот же склад → SAME_WAREHOUSES")
    void sameWarehousesAreRejected() {
        // TODO: transfer(a, a, position(x, "1")) → SAME_WAREHOUSES.
        fail("TODO: implement test");
    }

    @Test
    @Disabled("TODO: implement test")
    @DisplayName("Недостаточный остаток → INSUFFICIENT_STOCK, учитывается сумма позиций одного товара")
    void insufficientStockIsRejected() {
        // TODO 1: Остаток X на A = 10. Отгрузка 11 → INSUFFICIENT_STOCK.
        // TODO 2: Отгрузка 6 + 6 одного товара → INSUFFICIENT_STOCK (каждая позиция по отдельности проходит).
        // TODO 3: Приёмка без остатка проходит — приход не проверяется.
        fail("TODO: implement test");
    }

    @Test
    @Disabled("TODO: implement test")
    @DisplayName("После ошибки цепочка прекращается: следующее звено не вызвано, движения не рассчитаны")
    void chainStopsAfterFirstError() {
        // TODO 1: DocumentValidationChain.of(new PositionValidator(), new RecordingValidator()) на пустом документе.
        // TODO 2: Код EMPTY_DOCUMENT, RecordingValidator.invocations() == 0.
        // TODO 3: defaultChain() на пустом документе: context.movementsCalculated() == false.
        fail("TODO: implement test");
    }

    @Test
    @Disabled("TODO: implement test")
    @DisplayName("Корректные приёмка, отгрузка и перемещение проходят все проверки")
    void validDocumentsPass() {
        // TODO: assertThatCode(() -> chain.validate(...)).doesNotThrowAnyException() для трёх типов.
        fail("TODO: implement test");
    }

    @Test
    @Disabled("TODO: implement test")
    @DisplayName("Стандартная цепочка: структурные проверки раньше проверки остатков")
    void defaultChainOrder() {
        // TODO: defaultChain().links() → классы Position, Quantity, Warehouse, StockAvailability по порядку.
        fail("TODO: implement test");
    }

    @Test
    @Disabled("TODO: implement test")
    @DisplayName("Цепочку можно расширить новым звеном без изменения существующих")
    void chainIsExtensible() {
        // TODO: of(new PositionValidator(), ..., RecordingValidator.failingWith(DUPLICATE_POSITION))
        //  → на корректном документе код DUPLICATE_POSITION.
        fail("TODO: implement test");
    }
}
