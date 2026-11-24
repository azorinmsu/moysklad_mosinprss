package ru.moysklad.concurrency.synchronizers;

import java.util.HashMap;
import java.util.Map;

/**
 * Блокировка чтения и записи для каталога товаров: много читателей цен,
 * один поток обновления.
 *
 * <p>Политика: предпочтение писателя. Пока есть ожидающий писатель,
 * новый читатель не входит. Это исключает голодание писателя при непрерывном
 * потоке читателей. Повторный захват той же блокировки тем же потоком
 * не поддерживается. Нельзя повышать читателя до писателя.
 *
 * <p>Контракт:
 * <ul>
 *   <li>несколько потоков могут держать чтение одновременно;</li>
 *   <li>писатель только один, и в этот момент читателей нет;</li>
 *   <li>{@link #unlockRead()} и {@link #unlockWrite()} разрешены только владельцу,
 *       иначе {@link IllegalMonitorStateException};</li>
 *   <li>прерывание ожидания завершается {@link InterruptedException}.</li>
 * </ul>
 *
 * <p>Не делегируйте работу {@link java.util.concurrent.locks.ReentrantReadWriteLock}.
 */
public class CustomReadWriteLock {

    private int readers;
    private final Map<Thread, Integer> readHolders = new HashMap<>();
    private Thread writer;
    private int waitingWriters;

    public synchronized void lockRead() throws InterruptedException {
        while (writer != null || waitingWriters > 0)
            wait();
        readers++;
        readHolders.merge(Thread.currentThread(), 1, Integer::sum);
    }

    public synchronized void unlockRead() {
        Thread self = Thread.currentThread();
        Integer held = readHolders.get(self);
        if (held == null)
            throw new IllegalMonitorStateException("Блокировка чтения не удерживается: " + self.getName());
        readers--;

        if (held == 1)
            readHolders.remove(self);
        else
            readHolders.put(self, held - 1);

        if (readers == 0)
            notifyAll();
    }

    public synchronized void lockWrite() throws InterruptedException {
        Thread self = Thread.currentThread();
        waitingWriters++;
        try {
            while (writer != null || readers > 0) {
                wait();
            }
            writer = self;
        } finally {
            waitingWriters--;
            notifyAll();
        }
    }

    public synchronized void unlockWrite() {
        if (writer != Thread.currentThread())
            throw new IllegalMonitorStateException(
                    "Блокировка записи не удерживается: " + Thread.currentThread().getName());
        writer = null;
        notifyAll();
    }
}
