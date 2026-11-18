package ru.moysklad.concurrency.bugs;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import ru.moysklad.concurrency.domain.OrderStatus;
import ru.moysklad.concurrency.domain.SalesOrder;
import ru.moysklad.concurrency.events.EventProcessingService;
import ru.moysklad.concurrency.inventory.InventoryService;
import ru.moysklad.concurrency.inventory.StockLedger;
import ru.moysklad.concurrency.orders.PurchaseOrderService;
import ru.moysklad.concurrency.orders.SalesOrderService;
import ru.moysklad.concurrency.pricing.PriceService;
import ru.moysklad.concurrency.reports.ReportService;
import ru.moysklad.concurrency.reservation.ReservationService;
import ru.moysklad.concurrency.shipment.ShipmentService;
import ru.moysklad.concurrency.transfer.StockTransferService;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Tag("baseline")
@Timeout(10)
class ErpBaselineTest {

    @Test
    void openingBalanceAndPurchaseIncreaseStock() {
        StockLedger ledger = new StockLedger();
        InventoryService inventory = new InventoryService(ledger);
        inventory.acceptOpeningBalance("paper-a4", "main", 10);
        PurchaseOrderService purchases = new PurchaseOrderService(inventory);
        var order = purchases.receive("supplier-1", "paper-a4", "main", 4);
        assertThat(order.status()).isEqualTo(OrderStatus.RECEIVED);
        assertThat(ledger.require("paper-a4", "main").getQuantity()).isEqualTo(14);
        assertThat(purchases.orders()).hasSize(1);
    }

    @Test
    void salesOrderReservesStock() {
        StockLedger ledger = new StockLedger();
        InventoryService inventory = new InventoryService(ledger);
        inventory.acceptOpeningBalance("paper-a4", "main", 10);
        ReservationService reservations = new ReservationService(ledger);
        SalesOrderService sales = new SalesOrderService(reservations);
        SalesOrder order = sales.create("customer-1", "org-A", "paper-a4", "main", 3);
        assertThat(order.status()).isEqualTo(OrderStatus.RESERVED);
        assertThat(ledger.require("paper-a4", "main").available()).isEqualTo(7);
        assertThat(sales.orderCount("customer-1")).isEqualTo(1);
    }

    @Test
    void shipmentWritesOffAndTransferKeepsTheSum() {
        StockLedger ledger = new StockLedger();
        ledger.put("paper-a4", "main", 10);
        ledger.put("paper-a4", "secondary", 1);
        new ShipmentService(ledger).ship("SO-1", "paper-a4", "main", 2);
        new StockTransferService(ledger).transfer("paper-a4", "main", "secondary", 3);
        assertThat(ledger.require("paper-a4", "main").getQuantity()).isEqualTo(5);
        assertThat(ledger.require("paper-a4", "secondary").getQuantity()).isEqualTo(4);
    }

    @Test
    void priceAndMovementReportAreVisible() {
        StockLedger ledger = new StockLedger();
        ledger.put("paper-a4", "main", 10);
        PriceService prices = new PriceService();
        prices.setSalePrice("paper-a4", new BigDecimal("12.50"), new BigDecimal("5"));
        assertThat(prices.price("paper-a4").salePrice()).isEqualByComparingTo("12.50");
        ReportService reports = new ReportService(ledger);
        EventProcessingService events = new EventProcessingService(ledger, reports);
        events.apply("paper-a4", "main", -2, "sale");
        assertThat(ledger.require("paper-a4", "main").getQuantity()).isEqualTo(8);
        assertThat(reports.movementTurnover()).isEqualTo(-2);
        assertThat(reports.stockReport()).hasSize(1);
    }

    @Test
    void negativeOpeningBalanceIsRejected() {
        InventoryService inventory = new InventoryService(new StockLedger());
        assertThatThrownBy(() -> inventory.acceptOpeningBalance("paper-a4", "main", -1))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
