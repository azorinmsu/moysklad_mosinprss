package ru.moysklad.concurrency.support;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

public final class ConcurrentTasks {

    private ConcurrentTasks() {
    }

    public static ExecutorService pool(int threads, String name) {
        ThreadFactory factory = runnable -> {
            Thread thread = new Thread(runnable, name);
            thread.setDaemon(true);
            return thread;
        };
        return Executors.newFixedThreadPool(threads, factory);
    }

    public static void shutdown(ExecutorService executor) {
        executor.shutdownNow();
        try {
            executor.awaitTermination(2, TimeUnit.SECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            executor.shutdownNow();
        }
    }

    public static void awaitState(Thread thread, Thread.State state, long timeoutMillis) {
        long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMillis);
        while (thread.getState() != state) {
            if (System.nanoTime() >= deadline || !thread.isAlive()) {
                throw new AssertionError(
                        "Ожидалось состояние " + state + ", фактически " + thread.getState()
                );
            }
            Thread.onSpinWait();
        }
    }
}
