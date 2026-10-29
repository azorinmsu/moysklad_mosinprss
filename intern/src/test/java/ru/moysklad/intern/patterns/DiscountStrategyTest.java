package ru.moysklad.intern.patterns;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * Задание 1. Strategy — расчёт скидок.
 * Сравнивайте суммы с {@code OrderTestData.rub("...")}: так проверяется и значение, и масштаб (2 знака).
 */
class DiscountStrategyTest {

    @Test
    @Disabled("TODO: implement test")
    @DisplayName("REGULAR: скидка 0.00 при любой сумме")
    void regularCustomerGetsNoDiscount() {
        // TODO: RegularDiscountStrategy для 10 000 ₽ → 0.00
        fail("TODO: implement test");
    }

    @Test
    @Disabled("TODO: implement test")
    @DisplayName("WHOLESALE ниже порога: скидки нет")
    void wholesaleBelowThresholdGetsNoDiscount() {
        // TODO: WholesaleDiscountStrategy для 49 999.99 ₽ → 0.00
        fail("TODO: implement test");
    }

    @Test
    @Disabled("TODO: implement test")
    @DisplayName("WHOLESALE на пороге: скидка 10%")
    void wholesaleAtThresholdGetsDiscount() {
        // TODO: WholesaleDiscountStrategy для 50 000.00 ₽ → 5 000.00
        fail("TODO: implement test");
    }

    @Test
    @Disabled("TODO: implement test")
    @DisplayName("WHOLESALE выше порога: скидка 10%")
    void wholesaleAboveThresholdGetsDiscount() {
        // TODO: WholesaleDiscountStrategy для 55 000 ₽ → 5 500.00
        fail("TODO: implement test");
    }

    @Test
    @Disabled("TODO: implement test")
    @DisplayName("VIP: скидка 15% независимо от суммы")
    void vipGetsFifteenPercent() {
        // TODO: VipDiscountStrategy для 25 000 ₽ → 3 750.00 и для 100 ₽ → 15.00
        fail("TODO: implement test");
    }

    @Test
    @Disabled("TODO: implement test")
    @DisplayName("Нулевая стоимость: скидка 0.00 у всех стратегий")
    void zeroSubtotalGivesZeroDiscount() {
        // TODO: для каждой из трёх стратегий subtotal 0 → 0.00
        fail("TODO: implement test");
    }

    @Test
    @Disabled("TODO: implement test")
    @DisplayName("Округление до копеек по правилу HALF_UP")
    void discountIsRoundedHalfUp() {
        // TODO:
        //  VIP: 333.33 × 0.15 = 49.9995 → 50.00
        //  WHOLESALE: 50 000.05 × 0.10 = 5 000.005 → 5 000.01
        //  Проверьте, что у результата scale() == 2.
        fail("TODO: implement test");
    }

    @Test
    @Disabled("TODO: implement test")
    @DisplayName("Скидка не отрицательна и не больше стоимости товаров")
    void discountIsWithinBounds() {
        // TODO: для нескольких сумм и всех стратегий 0 <= скидка <= subtotal
        fail("TODO: implement test");
    }

    @Test
    @Disabled("TODO: implement test")
    @DisplayName("null или отрицательная стоимость — IllegalArgumentException")
    void invalidSubtotalIsRejected() {
        // TODO: calculateDiscount(null) и calculateDiscount(-1) → IllegalArgumentException
        fail("TODO: implement test");
    }
}
