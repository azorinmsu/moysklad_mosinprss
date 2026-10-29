package ru.moysklad.intern.patterns.model;

import java.math.BigDecimal;

/**
 * Итоговый расчёт заказа покупателя. Все суммы в рублях, округлены по {@link Money#round(BigDecimal)}.
 *
 * @param subtotal              стоимость товаров до скидки
 * @param discount              сумма скидки (применяется только к товарам)
 * @param subtotalAfterDiscount стоимость товаров после скидки
 * @param deliveryCost          стоимость доставки ({@code 0.00}, если не выбрана)
 * @param packagingCost         стоимость подарочной упаковки ({@code 0.00}, если не выбрана)
 * @param total                 итого к оплате
 */
public record OrderCalculation(
        BigDecimal subtotal,
        BigDecimal discount,
        BigDecimal subtotalAfterDiscount,
        BigDecimal deliveryCost,
        BigDecimal packagingCost,
        BigDecimal total
) {
}
