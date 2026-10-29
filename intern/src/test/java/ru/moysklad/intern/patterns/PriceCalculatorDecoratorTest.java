package ru.moysklad.intern.patterns;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * Задание 4. Decorator — дополнительные услуги.
 * Для изоляции от скидок используйте в базовом калькуляторе RegularDiscountStrategy
 * или лямбду {@code subtotal -> Money.ZERO}.
 */
class PriceCalculatorDecoratorTest {

    @Test
    @Disabled("TODO: implement test")
    @DisplayName("Без услуг: стоимость товаров после скидки")
    void baseCalculatorWithoutServices() {
        // TODO: BasePriceCalculator(RegularDiscountStrategy) для createRegularOrder() → 2 500.00
        fail("TODO: implement test");
    }

    @Test
    @Disabled("TODO: implement test")
    @DisplayName("Только доставка: +500 ₽")
    void deliveryOnly() {
        // TODO: DeliveryDecorator(base) для заказа на 2 500 ₽ → 3 000.00
        fail("TODO: implement test");
    }

    @Test
    @Disabled("TODO: implement test")
    @DisplayName("Только упаковка: +200 ₽")
    void packagingOnly() {
        // TODO: PackagingDecorator(base) для заказа на 2 500 ₽ → 2 700.00
        fail("TODO: implement test");
    }

    @Test
    @Disabled("TODO: implement test")
    @DisplayName("Доставка + упаковка: +700 ₽")
    void deliveryAndPackaging() {
        // TODO: PackagingDecorator(DeliveryDecorator(base)) для заказа на 2 500 ₽ → 3 200.00
        fail("TODO: implement test");
    }

    @Test
    @Disabled("TODO: implement test")
    @DisplayName("Порядок фиксированных надбавок не влияет на итог")
    void decoratorOrderDoesNotMatter() {
        // TODO: Packaging(Delivery(base)) и Delivery(Packaging(base)) дают одинаковую сумму
        fail("TODO: implement test");
    }

    @Test
    @Disabled("TODO: implement test")
    @DisplayName("Скидка применяется только к товарам, не к услугам")
    void discountIsNotAppliedToServices() {
        // TODO: VIP-заказ на 25 000 ₽: Packaging(Delivery(BasePriceCalculator(VipDiscountStrategy)))
        //  → 21 250 + 500 + 200 = 21 950.00 (а не (25 000 + 700) × 0.85)
        fail("TODO: implement test");
    }

    @Test
    @Disabled("TODO: implement test")
    @DisplayName("Повторное добавление одной услуги запрещено")
    void sameServiceCannotBeAddedTwice() {
        // TODO: new DeliveryDecorator(new DeliveryDecorator(base)) → IllegalStateException
        fail("TODO: implement test");
    }
}
