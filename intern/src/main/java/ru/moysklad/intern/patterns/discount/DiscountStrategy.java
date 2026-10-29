package ru.moysklad.intern.patterns.discount;

import java.math.BigDecimal;

/**
 * Strategy: алгоритм расчёта скидки для одного типа покупателя.
 * <p>
 * Контракт для всех реализаций:
 * <ul>
 *     <li>{@code subtotal} — стоимость товаров до доставки и упаковки;</li>
 *     <li>{@code subtotal == null} или отрицательный — {@link IllegalArgumentException};</li>
 *     <li>результат — сумма скидки в рублях (не процент), не меньше {@code 0.00} и не больше {@code subtotal};</li>
 *     <li>результат округлён через {@link ru.moysklad.intern.patterns.model.Money#round(BigDecimal)}.</li>
 * </ul>
 */
public interface DiscountStrategy {

    BigDecimal calculateDiscount(BigDecimal subtotal);
}
