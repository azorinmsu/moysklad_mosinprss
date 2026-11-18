package ru.moysklad.concurrency.bugs;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import ru.moysklad.concurrency.jobs.ReportRefreshTask;

import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@Timeout(10)
class Bug02Test {

    @Test
    @Tag("bug")
    void reportRefreshStopsWhenRequested() throws Exception {
        String java = Path.of(System.getProperty("java.home"), "bin", "java").toString();
        Process process = new ProcessBuilder(
                java,
                "-cp",
                System.getProperty("java.class.path"),
                "ru.moysklad.concurrency.bugs.ReportRefreshHarness"
        ).redirectErrorStream(true).start();
        try {
            boolean finished = process.waitFor(6, TimeUnit.SECONDS);
            String output = new String(process.getInputStream().readAllBytes());
            if (!finished) {
                process.destroyForcibly();
                assertThat(finished).as("поток пересчёта не завершился, вывод: %s", output).isTrue();
            }
            assertThat(process.exitValue()).as("поток пересчёта обязан остановиться, вывод: %s", output).isZero();
        } finally {
            if (process.isAlive()) {
                process.destroyForcibly();
            }
        }
    }

    @Test
    @Tag("baseline")
    void stopRequestedBeforeStartPreventsTheLoop() throws Exception {
        ReportRefreshTask task = new ReportRefreshTask();
        task.requestStop();
        Thread worker = new Thread(task, "report-refresh-boundary");
        worker.start();
        worker.join(1_000);
        assertThat(worker.isAlive()).isFalse();
    }
}
