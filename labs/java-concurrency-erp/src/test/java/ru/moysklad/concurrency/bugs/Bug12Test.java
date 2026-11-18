package ru.moysklad.concurrency.bugs;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import ru.moysklad.concurrency.domain.WarehouseReport;
import ru.moysklad.concurrency.inventory.StockLedger;
import ru.moysklad.concurrency.reports.ReportService;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Timeout(10)
class Bug12Test {

    @Test
    @Tag("bug")
    void reportCompletesWhenOneWarehouseFails() throws Exception {
        ReportService service = new ReportService(new StockLedger());
        WarehouseReport report = service.collectWarehouseBalances(List.of("A", "B", "C"), warehouseId -> {
            if ("B".equals(warehouseId)) {
                throw new IllegalStateException("нет связи со складом");
            }
            return 4;
        });
        assertThat(report.completed()).isTrue();
        assertThat(report.failedWarehouses()).containsExactly("B");
        assertThat(report.balances()).containsExactlyInAnyOrder(4, 4);
    }

    @Test
    @Tag("baseline")
    void reportCompletesWhenEveryWarehouseResponds() throws Exception {
        ReportService service = new ReportService(new StockLedger());
        WarehouseReport report = service.collectWarehouseBalances(List.of("A", "B"), warehouseId -> 3);
        assertThat(report.completed()).isTrue();
        assertThat(report.failedWarehouses()).isEmpty();
        assertThat(report.balances()).containsExactlyInAnyOrder(3, 3);
    }
}
