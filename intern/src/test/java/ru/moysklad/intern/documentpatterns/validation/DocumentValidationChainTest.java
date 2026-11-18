package ru.moysklad.intern.documentpatterns.validation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.moysklad.intern.documentpatterns.model.AcceptanceDocument;
import ru.moysklad.intern.documentpatterns.model.DocumentErrorCode;
import ru.moysklad.intern.documentpatterns.model.ShipmentDocument;
import ru.moysklad.intern.documentpatterns.model.TransferDocument;
import ru.moysklad.intern.documentpatterns.repository.InMemoryStockRepository;
import ru.moysklad.intern.documentpatterns.strategy.AcceptanceMovementStrategy;
import ru.moysklad.intern.documentpatterns.strategy.ShipmentMovementStrategy;
import ru.moysklad.intern.documentpatterns.strategy.TransferMovementStrategy;
import ru.moysklad.intern.documentpatterns.support.RecordingValidator;
import ru.moysklad.intern.documentpatterns.support.TestStocks;
import ru.moysklad.intern.entity.Product;
import ru.moysklad.intern.entity.warehouse.Warehouse;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static ru.moysklad.intern.documentpatterns.support.TestDocuments.acceptance;
import static ru.moysklad.intern.documentpatterns.support.TestDocuments.position;
import static ru.moysklad.intern.documentpatterns.support.TestDocuments.product;
import static ru.moysklad.intern.documentpatterns.support.TestDocuments.shipment;
import static ru.moysklad.intern.documentpatterns.support.TestDocuments.transfer;
import static ru.moysklad.intern.documentpatterns.support.TestDocuments.warehouse;

/**
 * Часть 3. Chain of Responsibility — проверки документа.
 * Проверяйте не только тип исключения, но и {@code DocumentValidationException#getCode()}.
 */
class DocumentValidationChainTest {

    @Test
    @DisplayName("Пустой документ → EMPTY_DOCUMENT")
    void emptyDocumentIsRejected() {
        Warehouse a = warehouse("A");
        AcceptanceDocument doc = acceptance(a);
        ValidationContext context = new ValidationContext(TestStocks.empty(), List::of);

        assertThatThrownBy(() -> DocumentValidationChain.defaultChain().validate(doc, context))
                .isInstanceOf(DocumentValidationException.class)
                .extracting(e -> ((DocumentValidationException) e).getCode())
                .isEqualTo(DocumentErrorCode.EMPTY_DOCUMENT);
    }

    @Test
    @DisplayName("Нулевое и отрицательное количество → INVALID_QUANTITY")
    void invalidQuantityIsRejected() {
        Warehouse a = warehouse("A");
        Product x = product("X");

        AcceptanceDocument docZero = acceptance(a, position(x, "0"));
        ValidationContext ctxZero = new ValidationContext(TestStocks.empty(), List::of);
        assertThatThrownBy(() -> DocumentValidationChain.defaultChain().validate(docZero, ctxZero))
                .isInstanceOf(DocumentValidationException.class)
                .extracting(e -> ((DocumentValidationException) e).getCode())
                .isEqualTo(DocumentErrorCode.INVALID_QUANTITY);

        AcceptanceDocument docNeg = acceptance(a, position(x, "-1"));
        ValidationContext ctxNeg = new ValidationContext(TestStocks.empty(), List::of);
        assertThatThrownBy(() -> DocumentValidationChain.defaultChain().validate(docNeg, ctxNeg))
                .isInstanceOf(DocumentValidationException.class)
                .extracting(e -> ((DocumentValidationException) e).getCode())
                .isEqualTo(DocumentErrorCode.INVALID_QUANTITY);
    }

    @Test
    @DisplayName("Отсутствующий склад → MISSING_TARGET_WAREHOUSE / MISSING_SOURCE_WAREHOUSE")
    void missingWarehouseIsRejected() {
        Product x = product("X");
        Warehouse a = warehouse("A");

        AcceptanceDocument accNull = acceptance(null, position(x, "10"));
        assertThatThrownBy(() -> DocumentValidationChain.defaultChain().validate(accNull, new ValidationContext(TestStocks.empty(), List::of)))
                .isInstanceOf(DocumentValidationException.class)
                .extracting(e -> ((DocumentValidationException) e).getCode())
                .isEqualTo(DocumentErrorCode.MISSING_TARGET_WAREHOUSE);

        ShipmentDocument shipNull = shipment(null, position(x, "10"));
        assertThatThrownBy(() -> DocumentValidationChain.defaultChain().validate(shipNull, new ValidationContext(TestStocks.empty(), List::of)))
                .isInstanceOf(DocumentValidationException.class)
                .extracting(e -> ((DocumentValidationException) e).getCode())
                .isEqualTo(DocumentErrorCode.MISSING_SOURCE_WAREHOUSE);

        TransferDocument trNull = transfer(a, null, position(x, "10"));
        assertThatThrownBy(() -> DocumentValidationChain.defaultChain().validate(trNull, new ValidationContext(TestStocks.empty(), List::of)))
                .isInstanceOf(DocumentValidationException.class)
                .extracting(e -> ((DocumentValidationException) e).getCode())
                .isEqualTo(DocumentErrorCode.MISSING_TARGET_WAREHOUSE);
    }

    @Test
    @DisplayName("Перемещение на тот же склад → SAME_WAREHOUSES")
    void sameWarehousesAreRejected() {
        Warehouse a = warehouse("A");
        Product x = product("X");
        TransferDocument doc = transfer(a, a, position(x, "1"));

        assertThatThrownBy(() -> DocumentValidationChain.defaultChain().validate(doc, new ValidationContext(TestStocks.empty(), List::of)))
                .isInstanceOf(DocumentValidationException.class)
                .extracting(e -> ((DocumentValidationException) e).getCode())
                .isEqualTo(DocumentErrorCode.SAME_WAREHOUSES);
    }

    @Test
    @DisplayName("Недостаточный остаток → INSUFFICIENT_STOCK, учитывается сумма позиций одного товара")
    void insufficientStockIsRejected() {
        Warehouse a = warehouse("A");
        Product x = product("X");
        InMemoryStockRepository stocks = TestStocks.withQuantity(x, a, "10");

        ShipmentDocument ship11 = shipment(a, position(x, "11"));
        ValidationContext ctx11 = new ValidationContext(stocks, () -> new ShipmentMovementStrategy().calculateMovements(ship11));
        assertThatThrownBy(() -> DocumentValidationChain.defaultChain().validate(ship11, ctx11))
                .isInstanceOf(DocumentValidationException.class)
                .extracting(e -> ((DocumentValidationException) e).getCode())
                .isEqualTo(DocumentErrorCode.INSUFFICIENT_STOCK);

        ShipmentDocument ship6plus6 = shipment(a, position(x, "6"), position(x, "6"));
        ValidationContext ctx6plus6 = new ValidationContext(stocks, () -> new ShipmentMovementStrategy().calculateMovements(ship6plus6));
        assertThatThrownBy(() -> DocumentValidationChain.defaultChain().validate(ship6plus6, ctx6plus6))
                .isInstanceOf(DocumentValidationException.class)
                .extracting(e -> ((DocumentValidationException) e).getCode())
                .isEqualTo(DocumentErrorCode.INSUFFICIENT_STOCK);

        AcceptanceDocument acc = acceptance(a, position(x, "100"));
        ValidationContext ctxAcc = new ValidationContext(TestStocks.empty(), () -> new AcceptanceMovementStrategy().calculateMovements(acc));
        assertThatCode(() -> DocumentValidationChain.defaultChain().validate(acc, ctxAcc))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("После ошибки цепочка прекращается: следующее звено не вызвано, движения не рассчитаны")
    void chainStopsAfterFirstError() {
        Warehouse a = warehouse("A");
        AcceptanceDocument emptyDoc = acceptance(a);
        RecordingValidator recording = new RecordingValidator();
        DocumentValidationChain chain = DocumentValidationChain.of(new PositionValidator(), recording);
        ValidationContext ctx = new ValidationContext(TestStocks.empty(), () -> { throw new AssertionError("Should not calculate movements"); });

        assertThatThrownBy(() -> chain.validate(emptyDoc, ctx))
                .isInstanceOf(DocumentValidationException.class)
                .extracting(e -> ((DocumentValidationException) e).getCode())
                .isEqualTo(DocumentErrorCode.EMPTY_DOCUMENT);
        assertThat(recording.invocations()).isZero();

        ValidationContext defaultCtx = new ValidationContext(TestStocks.empty(), List::of);
        assertThatThrownBy(() -> DocumentValidationChain.defaultChain().validate(emptyDoc, defaultCtx))
                .isInstanceOf(DocumentValidationException.class);
        assertThat(defaultCtx.movementsCalculated()).isFalse();
    }

    @Test
    @DisplayName("Корректные приёмка, отгрузка и перемещение проходят все проверки")
    void validDocumentsPass() {
        Warehouse a = warehouse("A");
        Warehouse b = warehouse("B");
        Product x = product("X");
        InMemoryStockRepository stocks = TestStocks.withQuantity(x, a, "100");

        AcceptanceDocument acc = acceptance(a, position(x, "10"));
        ValidationContext ctxAcc = new ValidationContext(stocks, () -> new AcceptanceMovementStrategy().calculateMovements(acc));
        assertThatCode(() -> DocumentValidationChain.defaultChain().validate(acc, ctxAcc)).doesNotThrowAnyException();

        ShipmentDocument ship = shipment(a, position(x, "10"));
        ValidationContext ctxShip = new ValidationContext(stocks, () -> new ShipmentMovementStrategy().calculateMovements(ship));
        assertThatCode(() -> DocumentValidationChain.defaultChain().validate(ship, ctxShip)).doesNotThrowAnyException();

        TransferDocument tr = transfer(a, b, position(x, "10"));
        ValidationContext ctxTr = new ValidationContext(stocks, () -> new TransferMovementStrategy().calculateMovements(tr));
        assertThatCode(() -> DocumentValidationChain.defaultChain().validate(tr, ctxTr)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Стандартная цепочка: структурные проверки раньше проверки остатков")
    void defaultChainOrder() {
        List<AbstractDocumentValidator> links = DocumentValidationChain.defaultChain().links();
        assertThat(links).hasSize(4);
        assertThat(links.get(0)).isInstanceOf(PositionValidator.class);
        assertThat(links.get(1)).isInstanceOf(QuantityValidator.class);
        assertThat(links.get(2)).isInstanceOf(WarehouseValidator.class);
        assertThat(links.get(3)).isInstanceOf(StockAvailabilityValidator.class);
    }

    @Test
    @DisplayName("Цепочку можно расширить новым звеном без изменения существующих")
    void chainIsExtensible() {
        Warehouse a = warehouse("A");
        Product x = product("X");
        AcceptanceDocument doc = acceptance(a, position(x, "10"));
        DocumentValidationChain chain = DocumentValidationChain.of(
                new PositionValidator(),
                new QuantityValidator(),
                new WarehouseValidator(),
                RecordingValidator.failingWith(DocumentErrorCode.DUPLICATE_POSITION)
        );
        ValidationContext ctx = new ValidationContext(TestStocks.empty(), List::of);

        assertThatThrownBy(() -> chain.validate(doc, ctx))
                .isInstanceOf(DocumentValidationException.class)
                .extracting(e -> ((DocumentValidationException) e).getCode())
                .isEqualTo(DocumentErrorCode.DUPLICATE_POSITION);
    }
}
