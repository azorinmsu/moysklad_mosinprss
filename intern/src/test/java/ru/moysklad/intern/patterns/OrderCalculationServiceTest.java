package ru.moysklad.intern.patterns;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * Задание 5. Объединение паттернов в {@code OrderCalculationService}.
 * Сервис создаётся вручную:
 * {@code new OrderCalculationService(new DiscountStrategyFactory(), OrderValidationChain.createDefault())}.
 */
class OrderCalculationServiceTest {

    @Test
    @Disabled("TODO: implement test")
    @DisplayName("VIP с доставкой и упаковкой: пример из задания")
    void vipOrderFullCalculation() {
        // TODO: createVipOrder() →
        //  subtotal 25 000.00, discount 3 750.00, subtotalAfterDiscount 21 250.00,
        //  deliveryCost 500.00, packagingCost 200.00, total 21 950.00
        fail("TODO: implement test");
    }

    @Test
    @Disabled("TODO: implement test")
    @DisplayName("REGULAR без услуг: итог равен стоимости товаров")
    void regularOrderWithoutServices() {
        // TODO: createRegularOrder() → discount 0.00, deliveryCost 0.00, packagingCost 0.00, total 2 500.00
        fail("TODO: implement test");
    }

    @Test
    @Disabled("TODO: implement test")
    @DisplayName("WHOLESALE выше порога с доставкой")
    void wholesaleOrderWithDelivery() {
        // TODO: createWholesaleOrder() → subtotal 55 000.00, discount 5 500.00, deliveryCost 500.00,
        //  packagingCost 0.00, total 50 000.00
        fail("TODO: implement test");
    }

    @Test
    @Disabled("TODO: implement test")
    @DisplayName("Некорректный заказ не рассчитывается")
    void invalidOrderIsRejectedBeforeCalculation() {
        // TODO: createEmptyOrder() → OrderValidationException с кодом EMPTY_ORDER
        fail("TODO: implement test");
    }

    @Test
    @Disabled("TODO: implement test")
    @DisplayName("Тип покупателя без стратегии → UnsupportedCustomerTypeException")
    void unsupportedCustomerTypeIsReported() {
        // TODO: createOrderWithSubtotal(CORPORATE, "1000", Set.of()) → UnsupportedCustomerTypeException
        fail("TODO: implement test");
    }

    @Test
    @Disabled("TODO: implement test")
    @DisplayName("Итог согласован с составляющими расчёта")
    void totalIsConsistentWithParts() {
        // TODO: для нескольких фикстур
        //  subtotalAfterDiscount == subtotal - discount
        //  total == subtotalAfterDiscount + deliveryCost + packagingCost
        fail("TODO: implement test");
    }
}
