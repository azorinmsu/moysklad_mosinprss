package ru.moysklad.intern.patterns.pricing;

import ru.moysklad.intern.patterns.model.AdditionalService;

import java.util.Objects;

/**
 * Базовый декоратор: реализует {@link PriceCalculator}, хранит обёрнутый калькулятор
 * и услугу, стоимость которой добавляет.
 * <p>
 * Защита от ошибки сборки: если услуга уже есть в цепочке обёрток,
 * конструктор бросает {@link IllegalStateException} — одна услуга не может быть начислена дважды.
 */
public abstract class PriceCalculatorDecorator implements PriceCalculator {

    protected final PriceCalculator delegate;
    protected final AdditionalService service;

    protected PriceCalculatorDecorator(PriceCalculator delegate, AdditionalService service) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
        this.service = Objects.requireNonNull(service, "service");
        if (isApplied(delegate, service)) {
            throw new IllegalStateException("Услуга '" + service.title() + "' уже добавлена в расчёт");
        }
    }

    public AdditionalService service() {
        return service;
    }

    /**
     * Проверяет, есть ли в цепочке обёрток, начиная с {@code calculator}, декоратор для {@code service}.
     */
    public static boolean isApplied(PriceCalculator calculator, AdditionalService service) {
        PriceCalculator current = calculator;
        while (current instanceof PriceCalculatorDecorator decorator) {
            if (decorator.service == service) {
                return true;
            }
            current = decorator.delegate;
        }
        return false;
    }
}
