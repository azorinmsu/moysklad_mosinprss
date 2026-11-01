package ru.moysklad.intern.documentpatterns.validation;

import ru.moysklad.intern.documentpatterns.model.StockMovement;
import ru.moysklad.intern.documentpatterns.repository.StockKey;
import ru.moysklad.intern.documentpatterns.repository.StockRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Данные, нужные звеньям цепочки помимо самого документа. <b>Готовая инфраструктура.</b>
 *
 * <p>Плановые движения рассчитываются <b>лениво и один раз</b>: первое обращение к {@link #plannedMovements()}
 * вызывает стратегию, последующие возвращают тот же список. Поэтому обработчик может после валидации
 * взять движения из контекста и не считать их повторно, а стратегия не вызывается, если цепочка
 * остановилась раньше (например, на пустом документе).
 */
public final class ValidationContext {

    private final StockRepository stocks;
    private final Supplier<List<StockMovement>> movementsCalculator;
    private List<StockMovement> plannedMovements;

    public ValidationContext(StockRepository stocks, Supplier<List<StockMovement>> movementsCalculator) {
        this.stocks = Objects.requireNonNull(stocks, "stocks");
        this.movementsCalculator = Objects.requireNonNull(movementsCalculator, "movementsCalculator");
    }

    public BigDecimal availableQuantity(StockKey key) {
        return stocks.getQuantity(key);
    }

    public List<StockMovement> plannedMovements() {
        if (plannedMovements == null) {
            List<StockMovement> calculated = movementsCalculator.get();
            plannedMovements = List.copyOf(Objects.requireNonNull(calculated, "Стратегия вернула null"));
        }
        return plannedMovements;
    }

    public boolean movementsCalculated() {
        return plannedMovements != null;
    }
}
