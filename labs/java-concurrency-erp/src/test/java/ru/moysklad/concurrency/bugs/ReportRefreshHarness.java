package ru.moysklad.concurrency.bugs;

import ru.moysklad.concurrency.jobs.ReportRefreshTask;

import java.util.concurrent.TimeUnit;

/**
 * Отдельный процесс: цикл пересчёта не должен остаться в JVM тестов.
 * Пауза перед сигналом остановки даёт JIT скомпилировать цикл.
 * Это ограничение теста видимости, оно описано в STUDENT_TASKS.
 */
public final class ReportRefreshHarness {

    public static void main(String[] args) throws Exception {
        ReportRefreshTask task = new ReportRefreshTask();
        Thread worker = new Thread(task, "report-refresh");
        worker.setDaemon(true);
        worker.start();
        if (!task.awaitStarted(2, TimeUnit.SECONDS)) {
            System.exit(3);
        }
        Thread.sleep(800);
        task.requestStop();
        worker.join(1_000);
        System.exit(worker.isAlive() ? 2 : 0);
    }

    private ReportRefreshHarness() {
    }
}
