package ru.moysklad.intern.patterns;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.moysklad.intern.patterns.discount.DiscountStrategyFactory;
import ru.moysklad.intern.patterns.discount.UnsupportedCustomerTypeException;
import ru.moysklad.intern.patterns.model.CustomerType;
import ru.moysklad.intern.patterns.model.OrderCalculation;
import ru.moysklad.intern.patterns.model.OrderDraft;
import ru.moysklad.intern.patterns.service.OrderCalculationService;
import ru.moysklad.intern.patterns.validation.OrderValidationChain;
import ru.moysklad.intern.patterns.validation.OrderValidationException;
import ru.moysklad.intern.patterns.validation.ValidationErrorCode;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static ru.moysklad.intern.patterns.OrderTestData.rub;

/**
 * Задание 5. Объединение паттернов в {@code OrderCalculationService}.
 * Сервис создаётся вручную:
 * {@code new OrderCalculationService(new DiscountStrategyFactory(), OrderValidationChain.createDefault())}.
 */
class OrderCalculationServiceTest {

    private final OrderCalculationService service =
            new OrderCalculationService(new DiscountStrategyFactory(), OrderValidationChain.createDefault());

    @Test
    @DisplayName("VIP с доставкой и упаковкой: пример из задания")
    void vipOrderFullCalculation() {
        OrderCalculation calc = service.calculate(OrderTestData.createVipOrder());

        assertThat(calc.subtotal()).isEqualTo(rub("25000.00"));
        assertThat(calc.discount()).isEqualTo(rub("3750.00"));
        assertThat(calc.subtotalAfterDiscount()).isEqualTo(rub("21250.00"));
        assertThat(calc.deliveryCost()).isEqualTo(rub("500.00"));
        assertThat(calc.packagingCost()).isEqualTo(rub("200.00"));
        assertThat(calc.total()).isEqualTo(rub("21950.00"));
    }

    @Test
    @DisplayName("REGULAR без услуг: итог равен стоимости товаров")
    void regularOrderWithoutServices() {
        OrderCalculation calc = service.calculate(OrderTestData.createRegularOrder());

        assertThat(calc.subtotal()).isEqualTo(rub("2500.00"));
        assertThat(calc.discount()).isEqualTo(rub("0.00"));
        assertThat(calc.subtotalAfterDiscount()).isEqualTo(rub("2500.00"));
        assertThat(calc.deliveryCost()).isEqualTo(rub("0.00"));
        assertThat(calc.packagingCost()).isEqualTo(rub("0.00"));
        assertThat(calc.total()).isEqualTo(rub("2500.00"));
    }

    @Test
    @DisplayName("WHOLESALE выше порога с доставкой")
    void wholesaleOrderWithDelivery() {
        OrderCalculation calc = service.calculate(OrderTestData.createWholesaleOrder());

        assertThat(calc.subtotal()).isEqualTo(rub("55000.00"));
        assertThat(calc.discount()).isEqualTo(rub("5500.00"));
        assertThat(calc.subtotalAfterDiscount()).isEqualTo(rub("49500.00"));
        assertThat(calc.deliveryCost()).isEqualTo(rub("500.00"));
        assertThat(calc.packagingCost()).isEqualTo(rub("0.00"));
        assertThat(calc.total()).isEqualTo(rub("50000.00"));
    }

    @Test
    @DisplayName("Некорректный заказ не рассчитывается")
    void invalidOrderIsRejectedBeforeCalculation() {
        assertThatThrownBy(() -> service.calculate(OrderTestData.createEmptyOrder()))
                .isInstanceOf(OrderValidationException.class)
                .satisfies(e -> assertThat(((OrderValidationException) e).getCode())
                        .isEqualTo(ValidationErrorCode.EMPTY_ORDER));
    }

    @Test
    @DisplayName("Тип покупателя без стратегии → UnsupportedCustomerTypeException")
    void unsupportedCustomerTypeIsReported() {
        OrderDraft order = OrderTestData.createOrderWithSubtotal(CustomerType.CORPORATE, "1000", Set.of());

        assertThatThrownBy(() -> service.calculate(order))
                .isInstanceOf(UnsupportedCustomerTypeException.class)
                .satisfies(e -> assertThat(((UnsupportedCustomerTypeException) e).getCustomerType())
                        .isEqualTo(CustomerType.CORPORATE));
    }

    @Test
    @DisplayName("Итог согласован с составляющими расчёта")
    void totalIsConsistentWithParts() {
        List<OrderDraft> orders = List.of(
                OrderTestData.createRegularOrder(),
                OrderTestData.createWholesaleOrder(),
                OrderTestData.createVipOrder()
        );

        for (OrderDraft order : orders) {
            OrderCalculation calc = service.calculate(order);
            assertThat(calc.subtotalAfterDiscount()).isEqualTo(calc.subtotal().subtract(calc.discount()));
            assertThat(calc.total())
                    .isEqualTo(calc.subtotalAfterDiscount().add(calc.deliveryCost()).add(calc.packagingCost()));
        }
    }
}
