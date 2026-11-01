package ru.moysklad.intern.documentpatterns.support;

import ru.moysklad.intern.documentpatterns.model.StockMovement;
import ru.moysklad.intern.documentpatterns.repository.StockHistoryEntry;
import ru.moysklad.intern.documentpatterns.repository.StockKey;
import ru.moysklad.intern.documentpatterns.repository.StockRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/**
 * Хранилище-обёртка, которое читает остатки из делегата, а при {@link #apply} бросает заданное исключение.
 * Имитирует сбой записи, чтобы проверить отсутствие частичного проведения.
 */
public class FailingStockRepository implements StockRepository {

    private final StockRepository delegate;
    private final Supplier<? extends RuntimeException> failure;
    private final AtomicInteger applyAttempts = new AtomicInteger();

    public FailingStockRepository(StockRepository delegate, Supplier<? extends RuntimeException> failure) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
        this.failure = Objects.requireNonNull(failure, "failure");
    }

    @Override
    public BigDecimal getQuantity(StockKey key) {
        return delegate.getQuantity(key);
    }

    @Override
    public void apply(List<StockMovement> movements) {
        applyAttempts.incrementAndGet();
        throw failure.get();
    }

    @Override
    public List<StockHistoryEntry> history() {
        return delegate.history();
    }

    public int applyAttempts() {
        return applyAttempts.get();
    }
}
