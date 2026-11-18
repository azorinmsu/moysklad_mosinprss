package ru.moysklad.concurrency.bugs;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import ru.moysklad.concurrency.jobs.OrderEventProcessor;
import ru.moysklad.concurrency.support.ConcurrentTasks;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@Timeout(10)
class Bug15Test {

    @Test
    @Tag("bug")
    void nextTaskOnTheSameThreadDoesNotSeePreviousOrganization() throws Exception {
        OrderEventProcessor processor = new OrderEventProcessor();
        ExecutorService pool = ConcurrentTasks.pool(1, "org-context");
        try {
            pool.submit(() -> processor.processOrder("org-A", "SO-1")).get(2, TimeUnit.SECONDS);
            pool.submit(processor::runMaintenance).get(2, TimeUnit.SECONDS);
            assertThat(processor.audit()).contains("org-A:SO-1");
            assertThat(processor.audit()).contains("MAINTENANCE");
            assertThat(processor.audit()).doesNotContain("LEAK:org-A");
        } finally {
            ConcurrentTasks.shutdown(pool);
        }
    }

    @Test
    @Tag("baseline")
    void eachOrderRecordsItsOwnOrganization() throws Exception {
        OrderEventProcessor processor = new OrderEventProcessor();
        ExecutorService pool = ConcurrentTasks.pool(1, "org-context");
        try {
            pool.submit(() -> processor.processOrder("org-A", "SO-1")).get(2, TimeUnit.SECONDS);
            pool.submit(() -> processor.processOrder("org-B", "SO-2")).get(2, TimeUnit.SECONDS);
            assertThat(processor.audit()).containsExactly("org-A:SO-1", "org-B:SO-2");
        } finally {
            ConcurrentTasks.shutdown(pool);
        }
    }
}
