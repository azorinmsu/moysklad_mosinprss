package ru.moysklad.concurrency.synchronizers;

/**
 * Считающий семафор: ограничение числа параллельных запросов к внешней системе.
 *
 * <p>Контракт:
 * <ul>
 *   <li>{@link #acquire()} забирает разрешение или ждёт, пока оно появится;</li>
 *   <li>{@link #tryAcquire()} не блокируется;</li>
 *   <li>{@link #tryAcquire(long, java.util.concurrent.TimeUnit)} ждёт не дольше таймаута;</li>
 *   <li>{@link #release()} возвращает одно разрешение и будит ожидающий поток;</li>
 *   <li>разрешения не теряются и не появляются сами собой;</li>
 *   <li>прерывание во время ожидания завершается {@link InterruptedException}, разрешение при этом не забрано;</li>
 *   <li>несколько потоков конкурируют за один пул разрешений, лишние не входят в критическую секцию.</li>
 * </ul>
 *
 * <p>Не делегируйте работу {@link java.util.concurrent.Semaphore}.
 */
public class CustomSemaphore {

    public int permits;

    public CustomSemaphore(int permits) {
        if (permits < 0) {
            throw new IllegalArgumentException("permits < 0");
        }
        this.permits = permits;
    }

    public synchronized void acquire() throws InterruptedException {
        while (permits == 0) {
            wait();
        }
        permits--;
    }

    public synchronized boolean tryAcquire() {
        if (permits == 0)
            return false;
        permits--;
        return true;
    }

    public synchronized boolean tryAcquire(long timeout, java.util.concurrent.TimeUnit unit) throws InterruptedException {
        long remaining = unit.toNanos(timeout);
        while (permits == 0) {
            if (remaining <= 0) {
                return false;
            }
            long started = System.nanoTime();
            wait(remaining / 1_000_000L, (int) (remaining % 1_000_000L));
            remaining -= System.nanoTime() - started;
        }
        permits--;
        return true;
    }

    public synchronized void release() {
        permits++;
        notifyAll();
    }

    public synchronized int availablePermits() {
        return this.permits;
    }
}
