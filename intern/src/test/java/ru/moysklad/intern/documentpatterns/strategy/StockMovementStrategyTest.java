package ru.moysklad.intern.documentpatterns.strategy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.moysklad.intern.documentpatterns.model.AcceptanceDocument;
import ru.moysklad.intern.documentpatterns.model.ShipmentDocument;
import ru.moysklad.intern.documentpatterns.model.StockMovement;
import ru.moysklad.intern.documentpatterns.model.TransferDocument;
import ru.moysklad.intern.documentpatterns.repository.InMemoryStockRepository;
import ru.moysklad.intern.documentpatterns.support.TestStocks;
import ru.moysklad.intern.entity.Product;
import ru.moysklad.intern.entity.stock.Lot;
import ru.moysklad.intern.entity.warehouse.Warehouse;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static ru.moysklad.intern.documentpatterns.support.TestDocuments.acceptance;
import static ru.moysklad.intern.documentpatterns.support.TestDocuments.lot;
import static ru.moysklad.intern.documentpatterns.support.TestDocuments.position;
import static ru.moysklad.intern.documentpatterns.support.TestDocuments.product;
import static ru.moysklad.intern.documentpatterns.support.TestDocuments.qty;
import static ru.moysklad.intern.documentpatterns.support.TestDocuments.shipment;
import static ru.moysklad.intern.documentpatterns.support.TestDocuments.transfer;
import static ru.moysklad.intern.documentpatterns.support.TestDocuments.warehouse;

/**
 * Часть 2. Strategy — расчёт складских движений.
 */
class StockMovementStrategyTest {

    @Test
    @DisplayName("Приёмка 10 шт. X на A → одно движение A / X: +10")
    void acceptanceIncreasesStock() {
        Warehouse a = warehouse("A");
        Product x = product("X");
        AcceptanceDocument doc = acceptance(a, position(x, "10"));

        List<StockMovement> movements = new AcceptanceMovementStrategy().calculateMovements(doc);
        assertThat(movements).hasSize(1);
        StockMovement m = movements.getFirst();
        assertThat(m.documentId()).isEqualTo(doc.getId());
        assertThat(m.warehouse()).isEqualTo(a);
        assertThat(m.product()).isEqualTo(x);
        assertThat(m.delta()).isEqualByComparingTo(qty("10"));
        assertThat(m.isIncoming()).isTrue();
    }

    @Test
    @DisplayName("Отгрузка 7 шт. X с A → одно движение A / X: -7")
    void shipmentDecreasesStock() {
        Warehouse a = warehouse("A");
        Product x = product("X");
        ShipmentDocument doc = shipment(a, position(x, "7"));

        List<StockMovement> movements = new ShipmentMovementStrategy().calculateMovements(doc);
        assertThat(movements).hasSize(1);
        StockMovement m = movements.getFirst();
        assertThat(m.documentId()).isEqualTo(doc.getId());
        assertThat(m.warehouse()).isEqualTo(a);
        assertThat(m.product()).isEqualTo(x);
        assertThat(m.delta()).isEqualByComparingTo(qty("-7"));
        assertThat(m.isOutgoing()).isTrue();
    }

    @Test
    @DisplayName("Перемещение 5 шт. X с A на B → A: -5, B: +5")
    void transferMovesBetweenWarehouses() {
        Warehouse a = warehouse("A");
        Warehouse b = warehouse("B");
        Product x = product("X");
        TransferDocument doc = transfer(a, b, position(x, "5"));

        List<StockMovement> movements = new TransferMovementStrategy().calculateMovements(doc);
        assertThat(movements).hasSize(2);

        StockMovement out = movements.get(0);
        assertThat(out.warehouse()).isEqualTo(a);
        assertThat(out.product()).isEqualTo(x);
        assertThat(out.delta()).isEqualByComparingTo(qty("-5"));

        StockMovement in = movements.get(1);
        assertThat(in.warehouse()).isEqualTo(b);
        assertThat(in.product()).isEqualTo(x);
        assertThat(in.delta()).isEqualByComparingTo(qty("5"));

        assertThat(out.delta().add(in.delta())).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Несколько позиций: по движению на позицию в порядке позиций")
    void multiplePositions() {
        Warehouse a = warehouse("A");
        Warehouse b = warehouse("B");
        Product x = product("X");
        Product y = product("Y");

        AcceptanceDocument acc = acceptance(a, position(x, "10"), position(y, "5"));
        List<StockMovement> accMoves = new AcceptanceMovementStrategy().calculateMovements(acc);
        assertThat(accMoves).hasSize(2);
        assertThat(accMoves.get(0).product()).isEqualTo(x);
        assertThat(accMoves.get(0).delta()).isEqualByComparingTo(qty("10"));
        assertThat(accMoves.get(1).product()).isEqualTo(y);
        assertThat(accMoves.get(1).delta()).isEqualByComparingTo(qty("5"));

        TransferDocument tr = transfer(a, b, position(x, "10"), position(y, "5"));
        List<StockMovement> trMoves = new TransferMovementStrategy().calculateMovements(tr);
        assertThat(trMoves).hasSize(4);
        assertThat(trMoves.get(0).product()).isEqualTo(x);
        assertThat(trMoves.get(0).delta()).isEqualByComparingTo(qty("-10"));
        assertThat(trMoves.get(1).product()).isEqualTo(x);
        assertThat(trMoves.get(1).delta()).isEqualByComparingTo(qty("10"));
        assertThat(trMoves.get(2).product()).isEqualTo(y);
        assertThat(trMoves.get(2).delta()).isEqualByComparingTo(qty("-5"));
        assertThat(trMoves.get(3).product()).isEqualTo(y);
        assertThat(trMoves.get(3).delta()).isEqualByComparingTo(qty("5"));
    }

    @Test
    @DisplayName("Партия позиции переносится в движение")
    void lotIsCarriedToMovement() {
        Warehouse a = warehouse("A");
        Product x = product("X");
        Lot l = lot("L1");
        AcceptanceDocument doc = acceptance(a, position(x, "3", l));

        List<StockMovement> movements = new AcceptanceMovementStrategy().calculateMovements(doc);
        assertThat(movements).hasSize(1);
        assertThat(movements.getFirst().lot()).isEqualTo(l);
    }

    @Test
    @DisplayName("Расчёт движений не меняет хранилище остатков, результат неизменяем")
    void calculationDoesNotTouchRepository() {
        Warehouse a = warehouse("A");
        Warehouse b = warehouse("B");
        Product x = product("X");
        InMemoryStockRepository repo = TestStocks.withQuantity(x, a, "100");
        var snapshotBefore = repo.snapshot();
        var historyBefore = repo.history();

        AcceptanceDocument acc = acceptance(a, position(x, "10"));
        ShipmentDocument ship = shipment(a, position(x, "5"));
        TransferDocument tr = transfer(a, b, position(x, "7"));

        List<StockMovement> m1 = new AcceptanceMovementStrategy().calculateMovements(acc);
        List<StockMovement> m2 = new ShipmentMovementStrategy().calculateMovements(ship);
        List<StockMovement> m3 = new TransferMovementStrategy().calculateMovements(tr);

        assertThat(repo.snapshot()).isEqualTo(snapshotBefore);
        assertThat(repo.history()).isEqualTo(historyBefore);

        assertThatThrownBy(() -> m1.add(m1.getFirst()))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> m2.add(m2.getFirst()))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> m3.add(m3.getFirst()))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
