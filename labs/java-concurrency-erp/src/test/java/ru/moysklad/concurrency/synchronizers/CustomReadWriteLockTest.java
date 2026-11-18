package ru.moysklad.concurrency.synchronizers;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import ru.moysklad.concurrency.support.ConcurrentTasks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Tag("synchronizer")
@Timeout(10)
class CustomReadWriteLockTest {

    @Test
    void severalReadersHoldTheLockTogether() throws Exception {
        CustomReadWriteLock lock = new CustomReadWriteLock();
        CountDownLatch inside = new CountDownLatch(2);
        CountDownLatch overlapped = new CountDownLatch(2);
        CountDownLatch release = new CountDownLatch(1);
        AtomicInteger overlaps = new AtomicInteger();
        Runnable reader = () -> {
            try {
                lock.lockRead();
                inside.countDown();
                if (!inside.await(2, TimeUnit.SECONDS)) {
                    return;
                }
                overlaps.incrementAndGet();
                overlapped.countDown();
                release.await(2, TimeUnit.SECONDS);
                lock.unlockRead();
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            }
        };
        Thread first = daemon(reader, "reader-1");
        Thread second = daemon(reader, "reader-2");
        first.start();
        second.start();
        try {
            assertThat(overlapped.await(2, TimeUnit.SECONDS)).isTrue();
            assertThat(overlaps.get()).isEqualTo(2);
        } finally {
            release.countDown();
            first.join(2_000);
            second.join(2_000);
        }
    }

    @Test
    void writerExcludesAReader() throws Exception {
        CustomReadWriteLock lock = new CustomReadWriteLock();
        CountDownLatch writerInside = new CountDownLatch(1);
        CountDownLatch releaseWriter = new CountDownLatch(1);
        CountDownLatch readerDone = new CountDownLatch(1);
        Thread writer = daemon(() -> {
            try {
                lock.lockWrite();
                writerInside.countDown();
                releaseWriter.await(2, TimeUnit.SECONDS);
                lock.unlockWrite();
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            }
        }, "writer");
        writer.start();
        assertThat(writerInside.await(2, TimeUnit.SECONDS)).isTrue();
        Thread reader = daemon(() -> {
            try {
                lock.lockRead();
                readerDone.countDown();
                lock.unlockRead();
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            }
        }, "reader");
        reader.start();
        assertThat(readerDone.await(300, TimeUnit.MILLISECONDS)).isFalse();
        releaseWriter.countDown();
        assertThat(readerDone.await(2, TimeUnit.SECONDS)).isTrue();
        writer.join(2_000);
        reader.join(2_000);
    }

    @Test
    void unlockWithoutOwnershipFails() throws Exception {
        CustomReadWriteLock lock = new CustomReadWriteLock();
        assertThatThrownBy(lock::unlockRead).isInstanceOf(IllegalMonitorStateException.class);
        assertThatThrownBy(lock::unlockWrite).isInstanceOf(IllegalMonitorStateException.class);
        lock.lockWrite();
        java.util.concurrent.atomic.AtomicBoolean strangerSaw = new java.util.concurrent.atomic.AtomicBoolean();
        Thread stranger = daemon(() -> {
            try {
                lock.unlockWrite();
            } catch (IllegalMonitorStateException exception) {
                strangerSaw.set(true);
            }
        }, "stranger");
        stranger.start();
        stranger.join(2_000);
        lock.unlockWrite();
        assertThat(stranger.isAlive()).isFalse();
        assertThat(strangerSaw).isTrue();
    }

    @Test
    void writerIsPreferredOverAReaderThatArrivesLater() throws Exception {
        CustomReadWriteLock lock = new CustomReadWriteLock();
        CountDownLatch readerHolds = new CountDownLatch(1);
        CountDownLatch releaseReader = new CountDownLatch(1);
        List<String> order = Collections.synchronizedList(new ArrayList<>());
        Thread reader = daemon(() -> {
            try {
                lock.lockRead();
                readerHolds.countDown();
                releaseReader.await(2, TimeUnit.SECONDS);
                lock.unlockRead();
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            }
        }, "held-reader");
        reader.start();
        assertThat(readerHolds.await(2, TimeUnit.SECONDS)).isTrue();
        Thread writer = daemon(() -> {
            try {
                lock.lockWrite();
                order.add("writer");
                lock.unlockWrite();
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            }
        }, "waiting-writer");
        writer.start();
        ConcurrentTasks.awaitState(writer, Thread.State.WAITING, 2_000);
        Thread lateReader = daemon(() -> {
            try {
                lock.lockRead();
                order.add("reader");
                lock.unlockRead();
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            }
        }, "late-reader");
        lateReader.start();
        ConcurrentTasks.awaitState(lateReader, Thread.State.WAITING, 2_000);
        releaseReader.countDown();
        writer.join(2_000);
        lateReader.join(2_000);
        reader.join(2_000);
        assertThat(order).startsWith("writer");
    }

    @Test
    void waitingWriterIsInterruptible() throws Exception {
        CustomReadWriteLock lock = new CustomReadWriteLock();
        lock.lockRead();
        java.util.concurrent.atomic.AtomicBoolean interrupted = new java.util.concurrent.atomic.AtomicBoolean();
        Thread writer = daemon(() -> {
            try {
                lock.lockWrite();
            } catch (InterruptedException exception) {
                interrupted.set(true);
            }
        }, "interrupted-writer");
        writer.start();
        ConcurrentTasks.awaitState(writer, Thread.State.WAITING, 2_000);
        writer.interrupt();
        writer.join(2_000);
        lock.unlockRead();
        assertThat(writer.isAlive()).isFalse();
        assertThat(interrupted).isTrue();
    }

    @Test
    void writeLockIsExclusiveAgainstAnotherWriter() throws Exception {
        CustomReadWriteLock lock = new CustomReadWriteLock();
        CountDownLatch inside = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        CountDownLatch secondInside = new CountDownLatch(1);
        Thread first = daemon(() -> {
            try {
                lock.lockWrite();
                inside.countDown();
                release.await(2, TimeUnit.SECONDS);
                lock.unlockWrite();
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            }
        }, "writer-a");
        first.start();
        assertThat(inside.await(2, TimeUnit.SECONDS)).isTrue();
        Thread second = daemon(() -> {
            try {
                lock.lockWrite();
                secondInside.countDown();
                lock.unlockWrite();
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            }
        }, "writer-b");
        second.start();
        assertThat(secondInside.await(300, TimeUnit.MILLISECONDS)).isFalse();
        release.countDown();
        assertThat(secondInside.await(2, TimeUnit.SECONDS)).isTrue();
        first.join(2_000);
        second.join(2_000);
    }

    private static Thread daemon(Runnable runnable, String name) {
        Thread thread = new Thread(runnable, name);
        thread.setDaemon(true);
        return thread;
    }
}
