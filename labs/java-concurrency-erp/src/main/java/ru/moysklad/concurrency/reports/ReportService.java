package ru.moysklad.concurrency.reports;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.moysklad.concurrency.domain.StockBalance;
import ru.moysklad.concurrency.domain.StockMovement;
import ru.moysklad.concurrency.domain.WarehouseReport;
import ru.moysklad.concurrency.inventory.StockLedger;
import ru.moysklad.concurrency.support.SyncPoint;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Function;

@Service
public class ReportService {

    private final StockLedger ledger;
    private final SyncPoint syncPoint;
    private final ReentrantReadWriteLock turnoverLock = new ReentrantReadWriteLock();
    private final Map<String, Long> turnoverByWarehouse = new HashMap<>();
    private long turnoverTotal;
    private final List<StockMovement> movements = new CopyOnWriteArrayList<>();

    @Autowired
    public ReportService(StockLedger ledger) {
        this(ledger, SyncPoint.NOOP);
    }

    public ReportService(StockLedger ledger, SyncPoint syncPoint) {
        this.ledger = ledger;
        this.syncPoint = syncPoint;
    }

    public List<StockBalance> stockReport() {
        return ledger.snapshot();
    }

    /**
     * Добавляет оборот склада в общий агрегат.
     * Итог равен сумме всех переданных сумм, в том числе при параллельных обновлениях.
     */
    public void recordTurnover(String warehouseId, long amount) {
        if (amount < 0)
            throw new IllegalArgumentException("Оборот не может быть отрицательным");
        turnoverLock.writeLock().lock(); // здесь косячок
        try {
            long currentWarehouse = turnoverByWarehouse.getOrDefault(warehouseId, 0L);
            long currentTotal = turnoverTotal;
            syncPoint.rendezvous();
            turnoverByWarehouse.put(warehouseId, currentWarehouse + amount);
            turnoverTotal = currentTotal + amount;
        } finally {
            turnoverLock.writeLock().unlock();
        }
    }

    public long turnoverTotal() {
        turnoverLock.readLock().lock();
        try {
            return turnoverTotal;
        } finally {
            turnoverLock.readLock().unlock();
        }
    }

    public long turnoverOf(String warehouseId) {
        turnoverLock.readLock().lock();
        try {
            return turnoverByWarehouse.getOrDefault(warehouseId, 0L);
        } finally {
            turnoverLock.readLock().unlock();
        }
    }

    public void addMovement(StockMovement movement) {
        movements.add(movement);
    }

    /**
     * Сумма количеств движений.
     * Параллельное добавление движения не прерывает расчёт и не портит сумму:
     * результат совпадает с суммой согласованного снимка списка.
     */
    public long movementTurnover() {
        long sum = 0;
        boolean paused = false;
        for (StockMovement movement : movements) {
            if (!paused) {
                paused = true;
                syncPoint.signal("movement-visible");
                syncPoint.awaitSignal("movement-mutated", 700, TimeUnit.MILLISECONDS);
            }
            sum += movement.quantity();
        }
        return sum;
    }

    public int consolidatedTurnoverSequential(List<String> warehouseIds, Function<String, Integer> loader) {
        int sum = 0;
        for (String warehouseId : warehouseIds) {
            sum += loader.apply(warehouseId);
        }
        return sum;
    }

    /**
     * Сводит обороты нескольких складов.
     * Метод завершается и возвращает сумму вкладов всех складов.
     */
    public int consolidatedTurnover(List<String> warehouseIds, Function<String, Integer> loader) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(Math.max(1, warehouseIds.size()), runnable -> {
            Thread thread = new Thread(runnable, "consolidated-report");
            thread.setDaemon(true);
            return thread;
        });
        try {
            List<CompletableFuture<Integer>> parts = warehouseIds.stream()
                    .map(warehouseId -> CompletableFuture.supplyAsync(() -> loader.apply(warehouseId), pool))
                    .toList();
            CompletableFuture.allOf(parts.toArray(CompletableFuture[]::new)).get(2, TimeUnit.SECONDS);
            int sum = 0;
            for (CompletableFuture<Integer> part : parts) {
                sum += part.join();
            }
            return sum;
        } finally {
            pool.shutdownNow();
            pool.awaitTermination(500, TimeUnit.MILLISECONDS);
        }
    }

    /**
     * Собирает остатки складов.
     * Координация завершается и тогда, когда один склад вернул ошибку:
     * успешные остатки и список сбоев доступны вызывающему.
     */
    public WarehouseReport collectWarehouseBalances(
            List<String> warehouseIds,
            Function<String, Integer> loader
    ) throws InterruptedException {
        ExecutorService pool = Executors.newFixedThreadPool(Math.max(1, warehouseIds.size()), runnable -> {
            Thread thread = new Thread(runnable, "warehouse-report");
            thread.setDaemon(true);
            return thread;
        });
        CountDownLatch latch = new CountDownLatch(warehouseIds.size());
        List<Integer> balances = new CopyOnWriteArrayList<>();
        List<String> failed = new CopyOnWriteArrayList<>();
        try {
            for (String warehouseId : warehouseIds) {
                pool.submit(() -> {
                    try {
                        balances.add(loader.apply(warehouseId));
                    } catch (RuntimeException ex) {
                        failed.add(warehouseId);
                    } finally {
                        latch.countDown();
                    }
                });
            }
            boolean completed = latch.await(1, TimeUnit.SECONDS);
            return new WarehouseReport(List.copyOf(balances), List.copyOf(failed), completed);
        } finally {
            pool.shutdownNow();
            pool.awaitTermination(1, TimeUnit.SECONDS);
        }
    }
}
