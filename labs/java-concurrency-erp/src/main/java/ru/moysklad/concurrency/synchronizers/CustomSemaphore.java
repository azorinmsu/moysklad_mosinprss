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

    public CustomSemaphore(int permits) {
        if (permits < 0) {
            throw new IllegalArgumentException("permits < 0");
        }
    }

    public void acquire() throws InterruptedException {
        throw new UnsupportedOperationException("SYNC-02");
    }

    public boolean tryAcquire() {
        throw new UnsupportedOperationException("SYNC-02");
    }

    public boolean tryAcquire(long timeout, java.util.concurrent.TimeUnit unit) throws InterruptedException {
        throw new UnsupportedOperationException("SYNC-02");
    }

    public void release() {
        throw new UnsupportedOperationException("SYNC-02");
    }

    public int availablePermits() {
        throw new UnsupportedOperationException("SYNC-02");
    }
}
