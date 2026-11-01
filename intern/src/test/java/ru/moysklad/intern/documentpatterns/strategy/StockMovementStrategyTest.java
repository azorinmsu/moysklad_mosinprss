package ru.moysklad.intern.documentpatterns.strategy;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * Часть 2. Strategy — расчёт складских движений.
 */
class StockMovementStrategyTest {

    @Test
    @Disabled("TODO: implement test")
    @DisplayName("Приёмка 10 шт. X на A → одно движение A / X: +10")
    void acceptanceIncreasesStock() {
        // TODO 1: acceptance(a, position(x, "10")).
        // TODO 2: Проверить склад, товар, documentId и delta = +10.
        fail("TODO: implement test");
    }

    @Test
    @Disabled("TODO: implement test")
    @DisplayName("Отгрузка 7 шт. X с A → одно движение A / X: -7")
    void shipmentDecreasesStock() {
        // TODO: знак движения отрицательный, склад — источник.
        fail("TODO: implement test");
    }

    @Test
    @Disabled("TODO: implement test")
    @DisplayName("Перемещение 5 шт. X с A на B → A: -5, B: +5")
    void transferMovesBetweenWarehouses() {
        // TODO: ровно два движения, порядок «расход, приход», сумма дельт равна нулю.
        fail("TODO: implement test");
    }

    @Test
    @Disabled("TODO: implement test")
    @DisplayName("Несколько позиций: по движению на позицию в порядке позиций")
    void multiplePositions() {
        // TODO: приёмка X=10, Y=5 → [+10 X, +5 Y]; перемещение X, Y → 4 движения.
        fail("TODO: implement test");
    }

    @Test
    @Disabled("TODO: implement test")
    @DisplayName("Партия позиции переносится в движение")
    void lotIsCarriedToMovement() {
        // TODO: position(x, "3", lot) → movement.lot() == lot.
        fail("TODO: implement test");
    }

    @Test
    @Disabled("TODO: implement test")
    @DisplayName("Расчёт движений не меняет хранилище остатков, результат неизменяем")
    void calculationDoesNotTouchRepository() {
        // TODO 1: Репозиторий с остатком; snapshot() до расчёта.
        // TODO 2: Рассчитать движения всеми стратегиями.
        // TODO 3: snapshot() и history() не изменились; список движений нельзя изменить.
        fail("TODO: implement test");
    }
}
