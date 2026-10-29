package ru.moysklad.intern.patterns;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * Задание 3. Chain of Responsibility — проверки заказа.
 * Проверяйте не только тип исключения, но и {@code OrderValidationException#getCode()}.
 */
class OrderValidationChainTest {

    @Test
    @Disabled("TODO: implement test")
    @DisplayName("Пустой заказ → EMPTY_ORDER")
    void emptyOrderIsRejected() {
        // TODO: OrderValidationChain.createDefault().validate(createEmptyOrder()) → код EMPTY_ORDER
        fail("TODO: implement test");
    }

    @Test
    @Disabled("TODO: implement test")
    @DisplayName("Нулевое количество → INVALID_QUANTITY")
    void zeroQuantityIsRejected() {
        // TODO: createOrderWithInvalidQuantity() → код INVALID_QUANTITY
        fail("TODO: implement test");
    }

    @Test
    @Disabled("TODO: implement test")
    @DisplayName("Отрицательное количество → INVALID_QUANTITY")
    void negativeQuantityIsRejected() {
        // TODO: createOrderWithNegativeQuantity() → код INVALID_QUANTITY, в сообщении есть наименование товара
        fail("TODO: implement test");
    }

    @Test
    @Disabled("TODO: implement test")
    @DisplayName("Отрицательная цена → INVALID_PRICE")
    void negativePriceIsRejected() {
        // TODO: createOrderWithNegativePrice() → код INVALID_PRICE
        fail("TODO: implement test");
    }

    @Test
    @Disabled("TODO: implement test")
    @DisplayName("Превышение 1 000 000 ₽ → TOTAL_AMOUNT_EXCEEDED, ровно 1 000 000 ₽ допустимо")
    void totalAmountLimitIsEnforced() {
        // TODO: createOrderExceedingMaxAmount() → код TOTAL_AMOUNT_EXCEEDED;
        //  createOrderAtMaxAmount() проходит без исключения
        fail("TODO: implement test");
    }

    @Test
    @Disabled("TODO: implement test")
    @DisplayName("Корректный заказ проходит все проверки")
    void validOrderPasses() {
        // TODO: createRegularOrder(), createWholesaleOrder(), createVipOrder() проходят без исключения
        fail("TODO: implement test");
    }

    @Test
    @Disabled("TODO: implement test")
    @DisplayName("После ошибки следующие звенья не вызываются")
    void chainStopsAfterFirstError() {
        // TODO:
        //  1. Собрать цепочку EmptyOrderValidator → RecordingOrderValidator.
        //  2. Пустой заказ → OrderValidationException, RecordingOrderValidator.wasInvoked() == false.
        //  3. Корректный заказ → RecordingOrderValidator.wasInvoked() == true.
        fail("TODO: implement test");
    }

    @Test
    @Disabled("TODO: implement test")
    @DisplayName("Порядок звеньев: первым срабатывает раннее правило")
    void validatorsRunInDefinedOrder() {
        // TODO: заказ с отрицательной ценой и нулевым количеством одновременно
        //  → код INVALID_QUANTITY (QuantityValidator стоит раньше PriceValidator)
        fail("TODO: implement test");
    }
}
