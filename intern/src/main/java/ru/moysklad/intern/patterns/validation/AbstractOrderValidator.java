package ru.moysklad.intern.patterns.validation;

import ru.moysklad.intern.patterns.model.OrderDraft;

import java.util.Objects;

/**
 * Звено цепочки проверок: хранит ссылку на следующий обработчик.
 * <p>
 * Наследник в {@link #validate(OrderDraft)} проверяет своё правило и сам решает,
 * передавать ли заказ дальше через {@link #passToNext(OrderDraft)}.
 */
public abstract class AbstractOrderValidator implements OrderValidator {

    private OrderValidator next;

    /**
     * Делает {@code next} следующим звеном и возвращает его, чтобы цепочку можно было собрать в одно выражение:
     * {@code first.linkWith(second).linkWith(third)}.
     */
    public <T extends OrderValidator> T linkWith(T next) {
        Objects.requireNonNull(next, "next");
        if (next == this) {
            throw new IllegalArgumentException("Валидатор не может ссылаться сам на себя");
        }
        this.next = next;
        return next;
    }

    protected final void passToNext(OrderDraft order) {
        if (next != null) {
            next.validate(order);
        }
    }
}
