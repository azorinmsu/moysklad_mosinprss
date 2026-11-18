package ru.moysklad.concurrency.bugs;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import ru.moysklad.concurrency.inventory.StockLedger;
import ru.moysklad.concurrency.reports.ReportService;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Timeout(10)
class Bug13Test {

    @Test
    @Tag("bug")
    void consolidatedReportReturnsTheSum() throws Exception {
        ReportService service = new ReportService(new StockLedger());
        int total = service.consolidatedTurnover(List.of("main", "secondary"), warehouseId -> 10);
        assertThat(total).isEqualTo(20);
    }

    @Test
    @Tag("baseline")
    void sequentialConsolidationSumsWarehouses() {
        ReportService service = new ReportService(new StockLedger());
        int total = service.consolidatedTurnoverSequential(List.of("main", "secondary"), warehouseId -> 10);
        assertThat(total).isEqualTo(20);
    }
}
