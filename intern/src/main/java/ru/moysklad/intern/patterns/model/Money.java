package ru.moysklad.intern.patterns.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Единое правило работы с денежными суммами в лабораторной: рубли с копейками,
 * два знака после запятой, округление {@link RoundingMode#HALF_UP}.
 * <p>
 * Все денежные результаты (скидки, промежуточные и итоговые суммы) должны проходить через {@link #round(BigDecimal)}.
 */
public final class Money {

    public static final int SCALE = 2;
    public static final RoundingMode ROUNDING = RoundingMode.HALF_UP;
    public static final BigDecimal ZERO = BigDecimal.ZERO.setScale(SCALE, ROUNDING);

    private Money() {
    }

    public static BigDecimal round(BigDecimal amount) {
        Objects.requireNonNull(amount, "amount");
        return amount.setScale(SCALE, ROUNDING);
    }

    /**
     * Создаёт сумму из строки, например {@code Money.of("10000")} → {@code 10000.00}.
     */
    public static BigDecimal of(String amount) {
        return round(new BigDecimal(amount));
    }
}
