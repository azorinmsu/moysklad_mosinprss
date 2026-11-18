package ru.moysklad.concurrency.events;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.moysklad.concurrency.domain.StockBalance;
import ru.moysklad.concurrency.domain.StockMovement;
import ru.moysklad.concurrency.inventory.StockLedger;
import ru.moysklad.concurrency.reports.ReportService;

import java.util.UUID;

@Service
public class EventProcessingService {

    private final StockLedger ledger;
    private final ReportService reports;

    @Autowired
    public EventProcessingService(StockLedger ledger, ReportService reports) {
        this.ledger = ledger;
        this.reports = reports;
    }

    public StockMovement apply(String productId, String warehouseId, int delta, String reason) {
        if (delta == 0) {
            throw new IllegalArgumentException("Движение не может быть нулевым");
        }
        StockBalance stock = ledger.require(productId, warehouseId);
        int updated = stock.getQuantity() + delta;
        if (updated < 0) {
            throw new IllegalArgumentException("Движение уводит остаток в минус");
        }
        stock.setQuantity(updated);
        StockMovement movement = new StockMovement(
                UUID.randomUUID().toString(),
                productId,
                warehouseId,
                delta,
                reason
        );
        reports.addMovement(movement);
        return movement;
    }
}
