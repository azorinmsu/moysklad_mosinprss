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

    public CustomCountDownLatch(int count) {
        if (count < 0) {
            throw new IllegalArgumentException("count < 0");
        }
    }

    public void await() throws InterruptedException {
        throw new UnsupportedOperationException("SYNC-01");
    }

    public boolean await(long timeout, java.util.concurrent.TimeUnit unit) throws InterruptedException {
        throw new UnsupportedOperationException("SYNC-01");
    }

    public void countDown() {
        throw new UnsupportedOperationException("SYNC-01");
    }

    public long getCount() {
        throw new UnsupportedOperationException("SYNC-01");
    }
}
