package ru.moysklad.concurrency.jobs;

import org.springframework.stereotype.Service;

import java.util.concurrent.Semaphore;

/**
 * Ограничивает число одновременных обращений к внешней учётной системе.
 * Одновременно выполняется не больше permits операций.
 * Неудачный захват разрешения не увеличивает пул разрешений.
 */
@Service
public class ExternalSyncGateway {

    private final Semaphore semaphore;
    private final int limit;

    public ExternalSyncGateway() {
        this(2);
    }

    public ExternalSyncGateway(int limit) {
        if (limit <= 0) {
            throw new IllegalArgumentException("Лимит должен быть положительным");
        }
        this.limit = limit;
        this.semaphore = new Semaphore(limit);
    }

    public int limit() {
        return limit;
    }

    public int availablePermits() {
        return semaphore.availablePermits();
    }

    public boolean execute(Runnable action) {
        boolean acquired = false;
        try {
            acquired = semaphore.tryAcquire();
            if (!acquired) {
                return false;
            }
            action.run();
            return true;
        } finally {
            if (acquired)
                semaphore.release();
        }
    }
}
