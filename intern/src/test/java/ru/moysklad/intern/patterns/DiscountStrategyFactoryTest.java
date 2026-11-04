package ru.moysklad.intern.patterns;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.moysklad.intern.patterns.discount.DiscountStrategy;
import ru.moysklad.intern.patterns.discount.DiscountStrategyFactory;
import ru.moysklad.intern.patterns.discount.RegularDiscountStrategy;
import ru.moysklad.intern.patterns.discount.UnsupportedCustomerTypeException;
import ru.moysklad.intern.patterns.discount.VipDiscountStrategy;
import ru.moysklad.intern.patterns.discount.WholesaleDiscountStrategy;
import ru.moysklad.intern.patterns.model.CustomerType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Задание 2. Фабрика стратегий (Simple Factory).
 */
class DiscountStrategyFactoryTest {

    @Test
    @DisplayName("REGULAR → RegularDiscountStrategy")
    void createsRegularStrategy() {
        DiscountStrategyFactory factory = new DiscountStrategyFactory();
        DiscountStrategy strategy = factory.create(CustomerType.REGULAR);
        assertThat(strategy).isInstanceOf(RegularDiscountStrategy.class);
    }

    @Test
    @DisplayName("WHOLESALE → WholesaleDiscountStrategy")
    void createsWholesaleStrategy() {
        DiscountStrategyFactory factory = new DiscountStrategyFactory();
        DiscountStrategy strategy = factory.create(CustomerType.WHOLESALE);
        assertThat(strategy).isInstanceOf(WholesaleDiscountStrategy.class);
    }

    @Test
    @DisplayName("VIP → VipDiscountStrategy")
    void createsVipStrategy() {
        DiscountStrategyFactory factory = new DiscountStrategyFactory();
        DiscountStrategy strategy = factory.create(CustomerType.VIP);
        assertThat(strategy).isInstanceOf(VipDiscountStrategy.class);
    }

    @Test
    @DisplayName("Неподдерживаемый тип → UnsupportedCustomerTypeException")
    void unsupportedTypeIsRejected() {
        DiscountStrategyFactory factory = new DiscountStrategyFactory();
        assertThatThrownBy(() -> factory.create(CustomerType.CORPORATE))
                .isInstanceOf(UnsupportedCustomerTypeException.class)
                .satisfies(e -> assertThat(((UnsupportedCustomerTypeException) e).getCustomerType())
                        .isEqualTo(CustomerType.CORPORATE));
    }

    @Test
    @DisplayName("Тип не указан → IllegalArgumentException")
    void missingTypeIsRejected() {
        DiscountStrategyFactory factory = new DiscountStrategyFactory();
        assertThatThrownBy(() -> factory.create(null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
