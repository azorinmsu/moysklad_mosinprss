package ru.moysklad.intern.documentpatterns.repository;

import ru.moysklad.intern.documentpatterns.model.StockMovement;
import ru.moysklad.intern.entity.Product;
import ru.moysklad.intern.entity.warehouse.Warehouse;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Потокобезопасное in-memory хранилище остатков. <b>Готовая инфраструктура</b> — студент её не меняет.
 *
 * <ul>
 *     <li>ключ — {@link StockKey} (товар + склад + партия);</li>
 *     <li>дельты могут быть положительными и отрицательными;</li>
 *     <li>набор движений применяется атомарно под одной блокировкой записи;</li>
 *     <li>допустимость проверяется по <b>суммарной</b> дельте каждого ключа в наборе;</li>
 *     <li>если хотя бы один итоговый остаток отрицателен — {@link InsufficientStockException}, изменений нет;</li>
 *     <li>каждое применённое движение попадает в журнал {@link #history()}.</li>
 * </ul>
 *
 * <p>Репозиторий не знает типов документов и бизнес-правил проведения: какие движения применять,
 * решают стратегии и обработчики.
 */
public class InMemoryStockRepository implements StockRepository {

    private final Map<StockKey, BigDecimal> quantities = new HashMap<>();
    private final List<StockHistoryEntry> history = new ArrayList<>();
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final Clock clock;
    private long sequence;

    public InMemoryStockRepository() {
        this(Clock.systemUTC());
    }

    public InMemoryStockRepository(Clock clock) {
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    /** Начальная установка остатка (подготовка данных). В журнал движений не попадает. */
    public InMemoryStockRepository setQuantity(Product product, Warehouse warehouse, BigDecimal quantity) {
        return setQuantity(StockKey.of(product, warehouse), quantity);
    }

    public InMemoryStockRepository setQuantity(StockKey key, BigDecimal quantity) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(quantity, "quantity");
        if (quantity.signum() < 0) {
            throw new IllegalArgumentException("Остаток не может быть отрицательным: " + quantity);
        }
        lock.writeLock().lock();
        try {
            quantities.put(key, quantity);
        } finally {
            lock.writeLock().unlock();
        }
        return this;
    }

    @Override
    public BigDecimal getQuantity(StockKey key) {
        Objects.requireNonNull(key, "key");
        lock.readLock().lock();
        try {
            return quantities.getOrDefault(key, BigDecimal.ZERO);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void apply(List<StockMovement> movements) {
        if (movements == null) {
            throw new IllegalArgumentException("Набор движений не задан");
        }
        List<StockMovement> batch = List.copyOf(movements);
        Map<StockKey, BigDecimal> netDeltas = new LinkedHashMap<>();
        for (StockMovement movement : batch) {
            netDeltas.merge(StockKey.of(movement), movement.delta(), BigDecimal::add);
        }

        lock.writeLock().lock();
        try {
            for (Map.Entry<StockKey, BigDecimal> entry : netDeltas.entrySet()) {
                BigDecimal current = quantities.getOrDefault(entry.getKey(), BigDecimal.ZERO);
                if (current.add(entry.getValue()).signum() < 0) {
                    throw new InsufficientStockException(entry.getKey(), current, entry.getValue().negate());
                }
            }
            for (StockMovement movement : batch) {
                StockKey key = StockKey.of(movement);
                BigDecimal after = quantities.getOrDefault(key, BigDecimal.ZERO).add(movement.delta());
                quantities.put(key, after);
                history.add(new StockHistoryEntry(++sequence, movement, after, clock.instant()));
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public List<StockHistoryEntry> history() {
        lock.readLock().lock();
        try {
            return List.copyOf(history);
        } finally {
            lock.readLock().unlock();
        }
    }

    /** Неизменяемый снимок всех остатков — удобно сравнивать «до» и «после» в тестах атомарности. */
    public Map<StockKey, BigDecimal> snapshot() {
        lock.readLock().lock();
        try {
            return Map.copyOf(quantities);
        } finally {
            lock.readLock().unlock();
        }
    }
}
