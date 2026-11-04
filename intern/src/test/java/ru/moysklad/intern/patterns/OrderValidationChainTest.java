package ru.moysklad.intern.patterns;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.moysklad.intern.patterns.model.CustomerType;
import ru.moysklad.intern.patterns.model.OrderDraft;
import ru.moysklad.intern.patterns.validation.EmptyOrderValidator;
import ru.moysklad.intern.patterns.validation.OrderValidationChain;
import ru.moysklad.intern.patterns.validation.OrderValidationException;
import ru.moysklad.intern.patterns.validation.ValidationErrorCode;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Задание 3. Chain of Responsibility — проверки заказа.
 * Проверяйте не только тип исключения, но и {@code OrderValidationException#getCode()}.
 */
class OrderValidationChainTest {

    @Test
    @DisplayName("Пустой заказ → EMPTY_ORDER")
    void emptyOrderIsRejected() {
        OrderValidationChain chain = OrderValidationChain.createDefault();
        assertThatThrownBy(() -> chain.validate(OrderTestData.createEmptyOrder()))
                .isInstanceOf(OrderValidationException.class)
                .satisfies(e -> assertThat(((OrderValidationException) e).getCode())
                        .isEqualTo(ValidationErrorCode.EMPTY_ORDER));
    }

    @Test
    @DisplayName("Нулевое количество → INVALID_QUANTITY")
    void zeroQuantityIsRejected() {
        OrderValidationChain chain = OrderValidationChain.createDefault();
        assertThatThrownBy(() -> chain.validate(OrderTestData.createOrderWithInvalidQuantity()))
                .isInstanceOf(OrderValidationException.class)
                .satisfies(e -> assertThat(((OrderValidationException) e).getCode())
                        .isEqualTo(ValidationErrorCode.INVALID_QUANTITY));
    }

    @Test
    @DisplayName("Отрицательное количество → INVALID_QUANTITY")
    void negativeQuantityIsRejected() {
        OrderValidationChain chain = OrderValidationChain.createDefault();
        assertThatThrownBy(() -> chain.validate(OrderTestData.createOrderWithNegativeQuantity()))
                .isInstanceOf(OrderValidationException.class)
                .satisfies(e -> {
                    OrderValidationException ex = (OrderValidationException) e;
                    assertThat(ex.getCode()).isEqualTo(ValidationErrorCode.INVALID_QUANTITY);
                    assertThat(ex.getMessage()).contains("Ручка гелевая");
                });
    }

    @Test
    @DisplayName("Отрицательная цена → INVALID_PRICE")
    void negativePriceIsRejected() {
        OrderValidationChain chain = OrderValidationChain.createDefault();
        assertThatThrownBy(() -> chain.validate(OrderTestData.createOrderWithNegativePrice()))
                .isInstanceOf(OrderValidationException.class)
                .satisfies(e -> assertThat(((OrderValidationException) e).getCode())
                        .isEqualTo(ValidationErrorCode.INVALID_PRICE));
    }

    @Test
    @DisplayName("Превышение 1 000 000 ₽ → TOTAL_AMOUNT_EXCEEDED, ровно 1 000 000 ₽ допустимо")
    void totalAmountLimitIsEnforced() {
        OrderValidationChain chain = OrderValidationChain.createDefault();
        assertThatThrownBy(() -> chain.validate(OrderTestData.createOrderExceedingMaxAmount()))
                .isInstanceOf(OrderValidationException.class)
                .satisfies(e -> assertThat(((OrderValidationException) e).getCode())
                        .isEqualTo(ValidationErrorCode.TOTAL_AMOUNT_EXCEEDED));

        assertThatCode(() -> chain.validate(OrderTestData.createOrderAtMaxAmount()))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Корректный заказ проходит все проверки")
    void validOrderPasses() {
        OrderValidationChain chain = OrderValidationChain.createDefault();
        assertThatCode(() -> chain.validate(OrderTestData.createRegularOrder()))
                .doesNotThrowAnyException();
        assertThatCode(() -> chain.validate(OrderTestData.createWholesaleOrder()))
                .doesNotThrowAnyException();
        assertThatCode(() -> chain.validate(OrderTestData.createVipOrder()))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("После ошибки следующие звенья не вызываются")
    void chainStopsAfterFirstError() {
        EmptyOrderValidator emptyValidator = new EmptyOrderValidator();
        RecordingOrderValidator recordingValidator = emptyValidator.linkWith(new RecordingOrderValidator());

        assertThatThrownBy(() -> emptyValidator.validate(OrderTestData.createEmptyOrder()))
                .isInstanceOf(OrderValidationException.class);
        assertThat(recordingValidator.wasInvoked()).isFalse();

        emptyValidator.validate(OrderTestData.createRegularOrder());
        assertThat(recordingValidator.wasInvoked()).isTrue();
    }

    @Test
    @DisplayName("Порядок звеньев: первым срабатывает раннее правило")
    void validatorsRunInDefinedOrder() {
        OrderDraft order = OrderTestData.order(
                CustomerType.REGULAR,
                Set.of(),
                OrderTestData.item("Тест", "-100", "0")
        );
        OrderValidationChain chain = OrderValidationChain.createDefault();

        assertThatThrownBy(() -> chain.validate(order))
                .isInstanceOf(OrderValidationException.class)
                .satisfies(e -> assertThat(((OrderValidationException) e).getCode())
                        .isEqualTo(ValidationErrorCode.INVALID_QUANTITY));
    }
}
