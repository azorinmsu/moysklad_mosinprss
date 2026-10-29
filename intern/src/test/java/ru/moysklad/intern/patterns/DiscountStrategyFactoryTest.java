package ru.moysklad.intern.patterns;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * Задание 2. Фабрика стратегий (Simple Factory).
 */
class DiscountStrategyFactoryTest {

    @Test
    @Disabled("TODO: implement test")
    @DisplayName("REGULAR → RegularDiscountStrategy")
    void createsRegularStrategy() {
        // TODO: create(REGULAR) возвращает экземпляр RegularDiscountStrategy
        fail("TODO: implement test");
    }

    @Test
    @Disabled("TODO: implement test")
    @DisplayName("WHOLESALE → WholesaleDiscountStrategy")
    void createsWholesaleStrategy() {
        // TODO: create(WHOLESALE) возвращает экземпляр WholesaleDiscountStrategy
        fail("TODO: implement test");
    }

    @Test
    @Disabled("TODO: implement test")
    @DisplayName("VIP → VipDiscountStrategy")
    void createsVipStrategy() {
        // TODO: create(VIP) возвращает экземпляр VipDiscountStrategy
        fail("TODO: implement test");
    }

    @Test
    @Disabled("TODO: implement test")
    @DisplayName("Неподдерживаемый тип → UnsupportedCustomerTypeException")
    void unsupportedTypeIsRejected() {
        // TODO: create(CORPORATE) → UnsupportedCustomerTypeException, getCustomerType() == CORPORATE
        fail("TODO: implement test");
    }

    @Test
    @Disabled("TODO: implement test")
    @DisplayName("Тип не указан → IllegalArgumentException")
    void missingTypeIsRejected() {
        // TODO: create(null) → IllegalArgumentException
        fail("TODO: implement test");
    }
}
