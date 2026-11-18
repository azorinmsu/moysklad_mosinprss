package ru.moysklad.concurrency.bugs;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import ru.moysklad.concurrency.domain.DiscountRule;
import ru.moysklad.concurrency.domain.PricePolicy;
import ru.moysklad.concurrency.pricing.PriceService;
import ru.moysklad.concurrency.support.ConcurrencyTestProbe;
import ru.moysklad.concurrency.support.ConcurrentTasks;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@Timeout(10)
class Bug14Test {

    @Test
    @Tag("bug")
    void publishedPricePolicyIsFullyInitialized() throws Exception {
        ConcurrencyTestProbe probe = new ConcurrencyTestProbe();
        PriceService service = new PriceService(probe);
        List<DiscountRule> rules = List.of(new DiscountRule("VIP", new BigDecimal("10")));
        ExecutorService pool = ConcurrentTasks.pool(1, "price-policy");
        try {
            Future<?> update = pool.submit(() -> service.updatePolicy(rules, new BigDecimal("0.05")));
            try {
                assertThat(probe.awaitSignal("policy-published", 2, TimeUnit.SECONDS)).isTrue();
                PricePolicy seen = service.currentPolicy();
                assertThat(seen).isNotNull();
                assertThat(seen.initialized()).isTrue();
                assertThat(seen.rules()).containsExactlyElementsOf(rules);
                assertThat(seen.margin()).isEqualByComparingTo("0.05");
            } finally {
                probe.signal("policy-observed");
            }
            update.get(2, TimeUnit.SECONDS);
        } finally {
            ConcurrentTasks.shutdown(pool);
        }
    }

    @Test
    @Tag("baseline")
    void policyIsCompleteAfterUpdateReturns() {
        PriceService service = new PriceService();
        List<DiscountRule> rules = List.of(new DiscountRule("REGULAR", new BigDecimal("3")));
        service.updatePolicy(rules, new BigDecimal("0.10"));
        assertThat(service.currentPolicy().initialized()).isTrue();
        assertThat(service.currentPolicy().rules()).containsExactlyElementsOf(rules);
        assertThat(service.currentPolicy().margin()).isEqualByComparingTo("0.10");
    }
}
