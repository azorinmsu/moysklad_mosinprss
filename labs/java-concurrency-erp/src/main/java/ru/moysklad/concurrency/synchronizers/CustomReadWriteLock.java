package ru.moysklad.concurrency.synchronizers;

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

    public void lockRead() throws InterruptedException {
        throw new UnsupportedOperationException("SYNC-03");
    }

    public void unlockRead() {
        throw new UnsupportedOperationException("SYNC-03");
    }

    public void lockWrite() throws InterruptedException {
        throw new UnsupportedOperationException("SYNC-03");
    }

    public void unlockWrite() {
        throw new UnsupportedOperationException("SYNC-03");
    }
}
