package ru.moysklad.intern.patterns;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.moysklad.intern.entity.Product;
import ru.moysklad.intern.patterns.model.AdditionalService;
import ru.moysklad.intern.patterns.model.CustomerType;
import ru.moysklad.intern.patterns.model.OrderDraft;
import ru.moysklad.intern.patterns.model.OrderItem;
import ru.moysklad.intern.patterns.pricing.DeliveryDecorator;
import ru.moysklad.intern.patterns.pricing.PackagingDecorator;
import ru.moysklad.intern.patterns.pricing.PriceCalculator;
import ru.moysklad.intern.patterns.pricing.PriceCalculatorDecorator;
import ru.moysklad.intern.patterns.validation.OrderValidationException;
import ru.moysklad.intern.patterns.validation.ValidationErrorCode;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static ru.moysklad.intern.patterns.OrderTestData.rub;

/**
 * Проверяет готовую инфраструктуру лабораторной: модели, тестовые данные, связывание звеньев и обёрток.
 * Эти тесты включены и должны проходить ещё до начала работы студента.
 */
class PatternsInfrastructureTest {

    @Test
    @DisplayName("Фикстуры содержат ожидаемые типы покупателей, позиции и услуги")
    void fixturesHaveExpectedShape() {
        OrderDraft vip = OrderTestData.createVipOrder();
        assertThat(vip.customerType()).isEqualTo(CustomerType.VIP);
        assertThat(vip.items()).hasSize(2);
        assertThat(vip.additionalServices())
                .containsExactlyInAnyOrder(AdditionalService.DELIVERY, AdditionalService.GIFT_PACKAGING);

        assertThat(OrderTestData.createRegularOrder().customerType()).isEqualTo(CustomerType.REGULAR);
        assertThat(OrderTestData.createWholesaleOrder().customerType()).isEqualTo(CustomerType.WHOLESALE);
        assertThat(OrderTestData.createEmptyOrder().items()).isEmpty();
        assertThat(OrderTestData.createOrderWithInvalidQuantity().items().getFirst().quantity())
                .isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("subtotal — сумма цена × количество по позициям")
    void subtotalSumsLineTotals() {
        assertThat(OrderTestData.createRegularOrder().subtotal()).isEqualTo(rub("2500"));
        assertThat(OrderTestData.createWholesaleOrder().subtotal()).isEqualTo(rub("55000"));
        assertThat(OrderTestData.createVipOrder().subtotal()).isEqualTo(rub("25000"));
        assertThat(OrderTestData.createEmptyOrder().subtotal()).isEqualTo(rub("0"));
    }

    @Test
    @DisplayName("Цены услуг: доставка 500 ₽, упаковка 200 ₽")
    void additionalServicePrices() {
        assertThat(AdditionalService.DELIVERY.price()).isEqualTo(rub("500"));
        assertThat(AdditionalService.GIFT_PACKAGING.price()).isEqualTo(rub("200"));
    }

    @Test
    @DisplayName("Списки позиций и услуг в OrderDraft неизменяемы, null превращается в пустые коллекции")
    void orderDraftIsImmutable() {
        OrderDraft draft = new OrderDraft(UUID.randomUUID(), null, null, null);
        assertThat(draft.items()).isEmpty();
        assertThat(draft.additionalServices()).isEmpty();
        assertThatThrownBy(() -> OrderTestData.createVipOrder().items().clear())
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("OrderItem создаётся из каталожного Product")
    void orderItemFromProduct() {
        Product product = new Product();
        product.setId(UUID.randomUUID());
        product.setName("Кофемашина");
        product.setPrice(new BigDecimal("15990.00"));

        OrderItem item = OrderItem.fromProduct(product, new BigDecimal("2"));

        assertThat(item.productId()).isEqualTo(product.getId());
        assertThat(item.productName()).isEqualTo("Кофемашина");
        assertThat(item.lineTotal()).isEqualTo(rub("31980"));
    }

    @Test
    @DisplayName("Тестовое звено передаёт заказ дальше по цепочке")
    void recordingValidatorPassesToNext() {
        RecordingOrderValidator first = new RecordingOrderValidator();
        RecordingOrderValidator second = first.linkWith(new RecordingOrderValidator());

        first.validate(OrderTestData.createRegularOrder());

        assertThat(first.invocations()).isEqualTo(1);
        assertThat(second.invocations()).isEqualTo(1);
    }

    @Test
    @DisplayName("Одну услугу нельзя обернуть дважды, разные услуги комбинируются")
    void decoratorRejectsDuplicateService() {
        PriceCalculator base = order -> rub("1000");
        PriceCalculator withDelivery = new DeliveryDecorator(base);
        PriceCalculator withBoth = new PackagingDecorator(withDelivery);

        assertThat(PriceCalculatorDecorator.isApplied(withBoth, AdditionalService.DELIVERY)).isTrue();
        assertThat(PriceCalculatorDecorator.isApplied(withBoth, AdditionalService.GIFT_PACKAGING)).isTrue();
        assertThat(PriceCalculatorDecorator.isApplied(base, AdditionalService.DELIVERY)).isFalse();
        assertThatThrownBy(() -> new DeliveryDecorator(withBoth))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("Бизнес-исключение проверки хранит код ошибки")
    void validationExceptionKeepsCode() {
        OrderValidationException exception =
                new OrderValidationException(ValidationErrorCode.EMPTY_ORDER, "Заказ пуст");
        assertThat(exception.getCode()).isEqualTo(ValidationErrorCode.EMPTY_ORDER);
        assertThat(exception).hasMessage("Заказ пуст");
    }
}
