package ru.moysklad.concurrency.synchronizers;

/**
 * Защёлка обратного отсчёта для ожидания загрузки остатков со всех складов.
 *
 * <p>Контракт:
 * <ul>
 *   <li>{@link #await()} и {@link #await(long, java.util.concurrent.TimeUnit)} блокируют поток,
 *       пока счётчик не станет нулём;</li>
 *   <li>несколько ожидающих потоков освобождаются вместе;</li>
 *   <li>{@link #countDown()} уменьшает счётчик, на нуле будит всех ожидающих и дальше не уходит в минус;</li>
 *   <li>прерывание ожидающего потока завершает ожидание с {@link InterruptedException};</li>
 *   <li>таймаут возвращает {@code false}, если счётчик ещё не ноль, и {@code true}, если дождались;</li>
 *   <li>ложные пробуждения не отпускают поток раньше нуля.</li>
 * </ul>
 *
 * <p>Не делегируйте работу {@link java.util.concurrent.CountDownLatch}.
 * Допустимы {@code synchronized}, {@code wait}, {@code notifyAll}.
 */
public class CustomCountDownLatch {

    private long count;

    public CustomCountDownLatch(int count) {
        if (count < 0) {
            throw new IllegalArgumentException("count < 0");
        }
        this.count = count;
    }

    public synchronized void await() throws InterruptedException {
        while (count > 0) {
            wait();
        }

    }

    public synchronized boolean await(long timeout, java.util.concurrent.TimeUnit unit) throws InterruptedException {
        long remaining = unit.toNanos(timeout);
        while (count > 0) {
            if (remaining <= 0)
                return false;
            long started = System.nanoTime();
            wait(remaining / 1_000_000L, (int) (remaining % 1_000_000L));
            remaining -= System.nanoTime() - started;
        }
        return true;
    }

    public synchronized void countDown() {
        if (count > 0) {
            count--;
            if (count == 0)
                notifyAll();
        }
    }

    public synchronized long getCount() {
        return this.count;
    }
}
