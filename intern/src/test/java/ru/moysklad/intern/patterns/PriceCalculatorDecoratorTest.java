package ru.moysklad.intern.patterns;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.moysklad.intern.patterns.discount.RegularDiscountStrategy;
import ru.moysklad.intern.patterns.discount.VipDiscountStrategy;
import ru.moysklad.intern.patterns.model.OrderDraft;
import ru.moysklad.intern.patterns.pricing.BasePriceCalculator;
import ru.moysklad.intern.patterns.pricing.DeliveryDecorator;
import ru.moysklad.intern.patterns.pricing.PackagingDecorator;
import ru.moysklad.intern.patterns.pricing.PriceCalculator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static ru.moysklad.intern.patterns.OrderTestData.rub;

/**
 * Задание 4. Decorator — дополнительные услуги.
 * Для изоляции от скидок используйте в базовом калькуляторе RegularDiscountStrategy
 * или лямбду {@code subtotal -> Money.ZERO}.
 */
class PriceCalculatorDecoratorTest {

    @Test
    @DisplayName("Без услуг: стоимость товаров после скидки")
    void baseCalculatorWithoutServices() {
        PriceCalculator base = new BasePriceCalculator(new RegularDiscountStrategy());
        assertThat(base.calculate(OrderTestData.createRegularOrder())).isEqualTo(rub("2500.00"));
    }

    @Test
    @DisplayName("Только доставка: +500 ₽")
    void deliveryOnly() {
        PriceCalculator base = new BasePriceCalculator(new RegularDiscountStrategy());
        PriceCalculator withDelivery = new DeliveryDecorator(base);
        assertThat(withDelivery.calculate(OrderTestData.createRegularOrder())).isEqualTo(rub("3000.00"));
    }

    @Test
    @DisplayName("Только упаковка: +200 ₽")
    void packagingOnly() {
        PriceCalculator base = new BasePriceCalculator(new RegularDiscountStrategy());
        PriceCalculator withPackaging = new PackagingDecorator(base);
        assertThat(withPackaging.calculate(OrderTestData.createRegularOrder())).isEqualTo(rub("2700.00"));
    }

    @Test
    @DisplayName("Доставка + упаковка: +700 ₽")
    void deliveryAndPackaging() {
        PriceCalculator base = new BasePriceCalculator(new RegularDiscountStrategy());
        PriceCalculator calculator = new PackagingDecorator(new DeliveryDecorator(base));
        assertThat(calculator.calculate(OrderTestData.createRegularOrder())).isEqualTo(rub("3200.00"));
    }

    @Test
    @DisplayName("Порядок фиксированных надбавок не влияет на итог")
    void decoratorOrderDoesNotMatter() {
        PriceCalculator base = new BasePriceCalculator(new RegularDiscountStrategy());
        PriceCalculator order1 = new PackagingDecorator(new DeliveryDecorator(base));
        PriceCalculator order2 = new DeliveryDecorator(new PackagingDecorator(base));
        OrderDraft order = OrderTestData.createRegularOrder();
        assertThat(order1.calculate(order)).isEqualTo(order2.calculate(order));
    }

    @Test
    @DisplayName("Скидка применяется только к товарам, не к услугам")
    void discountIsNotAppliedToServices() {
        PriceCalculator base = new BasePriceCalculator(new VipDiscountStrategy());
        PriceCalculator calculator = new PackagingDecorator(new DeliveryDecorator(base));
        assertThat(calculator.calculate(OrderTestData.createVipOrder())).isEqualTo(rub("21950.00"));
    }

    @Test
    @DisplayName("Повторное добавление одной услуги запрещено")
    void sameServiceCannotBeAddedTwice() {
        PriceCalculator base = new BasePriceCalculator(new RegularDiscountStrategy());
        PriceCalculator withDelivery = new DeliveryDecorator(base);
        assertThatThrownBy(() -> new DeliveryDecorator(withDelivery))
                .isInstanceOf(IllegalStateException.class);
    }
}
