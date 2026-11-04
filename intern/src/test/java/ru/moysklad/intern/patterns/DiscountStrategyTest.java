package ru.moysklad.intern.patterns;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.moysklad.intern.patterns.discount.DiscountStrategy;
import ru.moysklad.intern.patterns.discount.RegularDiscountStrategy;
import ru.moysklad.intern.patterns.discount.VipDiscountStrategy;
import ru.moysklad.intern.patterns.discount.WholesaleDiscountStrategy;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static ru.moysklad.intern.patterns.OrderTestData.rub;

/**
 * Задание 1. Strategy — расчёт скидок.
 * Сравнивайте суммы с {@code OrderTestData.rub("...")}: так проверяется и значение, и масштаб (2 знака).
 */
class DiscountStrategyTest {

    @Test
    @DisplayName("REGULAR: скидка 0.00 при любой сумме")
    void regularCustomerGetsNoDiscount() {
        RegularDiscountStrategy strategy = new RegularDiscountStrategy();
        assertThat(strategy.calculateDiscount(rub("10000"))).isEqualTo(rub("0.00"));
    }

    @Test
    @DisplayName("WHOLESALE ниже порога: скидки нет")
    void wholesaleBelowThresholdGetsNoDiscount() {
        WholesaleDiscountStrategy strategy = new WholesaleDiscountStrategy();
        assertThat(strategy.calculateDiscount(rub("49999.99"))).isEqualTo(rub("0.00"));
    }

    @Test
    @DisplayName("WHOLESALE на пороге: скидка 10%")
    void wholesaleAtThresholdGetsDiscount() {
        WholesaleDiscountStrategy strategy = new WholesaleDiscountStrategy();
        assertThat(strategy.calculateDiscount(rub("50000.00"))).isEqualTo(rub("5000.00"));
    }

    @Test
    @DisplayName("WHOLESALE выше порога: скидка 10%")
    void wholesaleAboveThresholdGetsDiscount() {
        WholesaleDiscountStrategy strategy = new WholesaleDiscountStrategy();
        assertThat(strategy.calculateDiscount(rub("55000"))).isEqualTo(rub("5500.00"));
    }

    @Test
    @DisplayName("VIP: скидка 15% независимо от суммы")
    void vipGetsFifteenPercent() {
        VipDiscountStrategy strategy = new VipDiscountStrategy();
        assertThat(strategy.calculateDiscount(rub("25000"))).isEqualTo(rub("3750.00"));
        assertThat(strategy.calculateDiscount(rub("100"))).isEqualTo(rub("15.00"));
    }

    @Test
    @DisplayName("Нулевая стоимость: скидка 0.00 у всех стратегий")
    void zeroSubtotalGivesZeroDiscount() {
        assertThat(new RegularDiscountStrategy().calculateDiscount(BigDecimal.ZERO)).isEqualTo(rub("0.00"));
        assertThat(new WholesaleDiscountStrategy().calculateDiscount(BigDecimal.ZERO)).isEqualTo(rub("0.00"));
        assertThat(new VipDiscountStrategy().calculateDiscount(BigDecimal.ZERO)).isEqualTo(rub("0.00"));
    }

    @Test
    @DisplayName("Округление до копеек по правилу HALF_UP")
    void discountIsRoundedHalfUp() {
        BigDecimal vipDiscount = new VipDiscountStrategy().calculateDiscount(new BigDecimal("333.33"));
        assertThat(vipDiscount).isEqualTo(rub("50.00"));
        assertThat(vipDiscount.scale()).isEqualTo(2);

        BigDecimal wholesaleDiscount = new WholesaleDiscountStrategy().calculateDiscount(new BigDecimal("50000.05"));
        assertThat(wholesaleDiscount).isEqualTo(rub("5000.01"));
        assertThat(wholesaleDiscount.scale()).isEqualTo(2);
    }

    @Test
    @DisplayName("Скидка не отрицательна и не больше стоимости товаров")
    void discountIsWithinBounds() {
        List<DiscountStrategy> strategies = List.of(
                new RegularDiscountStrategy(),
                new WholesaleDiscountStrategy(),
                new VipDiscountStrategy()
        );
        List<BigDecimal> amounts = List.of(BigDecimal.ZERO, rub("100"), rub("50000"), rub("100000"));

        for (DiscountStrategy strategy : strategies) {
            for (BigDecimal amount : amounts) {
                BigDecimal discount = strategy.calculateDiscount(amount);
                assertThat(discount).isGreaterThanOrEqualTo(BigDecimal.ZERO);
                assertThat(discount).isLessThanOrEqualTo(amount);
            }
        }
    }

    @Test
    @DisplayName("null или отрицательная стоимость — IllegalArgumentException")
    void invalidSubtotalIsRejected() {
        List<DiscountStrategy> strategies = List.of(
                new RegularDiscountStrategy(),
                new WholesaleDiscountStrategy(),
                new VipDiscountStrategy()
        );

        for (DiscountStrategy strategy : strategies) {
            assertThatThrownBy(() -> strategy.calculateDiscount(null))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> strategy.calculateDiscount(new BigDecimal("-1")))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
