package ru.moysklad.concurrency.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ru.moysklad.concurrency.domain.InsufficientStockException;
import ru.moysklad.concurrency.domain.Price;
import ru.moysklad.concurrency.domain.PurchaseOrder;
import ru.moysklad.concurrency.domain.ReservationResult;
import ru.moysklad.concurrency.domain.SalesOrder;
import ru.moysklad.concurrency.domain.Shipment;
import ru.moysklad.concurrency.domain.StockBalance;
import ru.moysklad.concurrency.domain.StockMovement;
import ru.moysklad.concurrency.domain.StockTransfer;
import ru.moysklad.concurrency.events.EventProcessingService;
import ru.moysklad.concurrency.inventory.InventoryService;
import ru.moysklad.concurrency.jobs.OrderEventProcessor;
import ru.moysklad.concurrency.orders.PurchaseOrderService;
import ru.moysklad.concurrency.orders.SalesOrderService;
import ru.moysklad.concurrency.pricing.PriceService;
import ru.moysklad.concurrency.reports.ReportService;
import ru.moysklad.concurrency.reservation.ReservationService;
import ru.moysklad.concurrency.shipment.ShipmentService;
import ru.moysklad.concurrency.transfer.StockTransferService;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api")
public class ErpController {

    private final InventoryService inventory;
    private final ReservationService reservations;
    private final SalesOrderService salesOrders;
    private final PurchaseOrderService purchaseOrders;
    private final ShipmentService shipments;
    private final StockTransferService transfers;
    private final PriceService prices;
    private final ReportService reports;
    private final EventProcessingService events;
    private final OrderEventProcessor orderEvents;

    public ErpController(
            InventoryService inventory,
            ReservationService reservations,
            SalesOrderService salesOrders,
            PurchaseOrderService purchaseOrders,
            ShipmentService shipments,
            StockTransferService transfers,
            PriceService prices,
            ReportService reports,
            EventProcessingService events,
            OrderEventProcessor orderEvents
    ) {
        this.inventory = inventory;
        this.reservations = reservations;
        this.salesOrders = salesOrders;
        this.purchaseOrders = purchaseOrders;
        this.shipments = shipments;
        this.transfers = transfers;
        this.prices = prices;
        this.reports = reports;
        this.events = events;
        this.orderEvents = orderEvents;
    }

    @GetMapping("/health")
    public String health() {
        return "ok";
    }

    @PostMapping("/stocks")
    public StockView openingBalance(@RequestBody StockChangeRequest request) {
        StockBalance balance = inventory.acceptOpeningBalance(
                request.productId(),
                request.warehouseId(),
                request.quantity()
        );
        return StockView.from(balance);
    }

    @PostMapping("/reservations")
    public ReservationResult reserve(@RequestBody ReserveRequest request) {
        return reservations.reserve(
                request.orderId(),
                request.productId(),
                request.warehouseId(),
                request.quantity()
        );
    }

    @PostMapping("/sales-orders")
    public SalesOrder createOrder(@RequestBody SalesOrderRequest request) {
        return salesOrders.create(
                request.customerId(),
                request.organizationId(),
                request.productId(),
                request.warehouseId(),
                request.quantity()
        );
    }

    @PostMapping("/shipments")
    public Shipment ship(@RequestBody ShipmentRequest request) {
        return shipments.ship(
                request.orderId(),
                request.productId(),
                request.warehouseId(),
                request.quantity()
        );
    }

    @PostMapping("/purchase-orders")
    public PurchaseOrder receive(@RequestBody PurchaseRequest request) {
        return purchaseOrders.receive(
                request.supplierId(),
                request.productId(),
                request.warehouseId(),
                request.quantity()
        );
    }

    @PostMapping("/transfers")
    public StockTransfer transfer(@RequestBody TransferRequest request) {
        return transfers.transfer(
                request.productId(),
                request.fromWarehouseId(),
                request.toWarehouseId(),
                request.quantity()
        );
    }

    @PutMapping("/prices/{productId}")
    public Price setPrice(@PathVariable String productId, @RequestBody PriceRequest request) {
        return prices.setSalePrice(productId, request.salePrice(), request.discountPercent());
    }

    @GetMapping("/reports/stock")
    public List<StockView> stockReport() {
        return reports.stockReport().stream().map(StockView::from).toList();
    }

    @PostMapping("/movements")
    public StockMovement movement(@RequestBody MovementRequest request) {
        return events.apply(request.productId(), request.warehouseId(), request.delta(), request.reason());
    }

    @PostMapping("/order-events")
    public void orderEvent(@RequestBody OrderEventRequest request) {
        orderEvents.processOrder(request.organizationId(), request.orderId());
    }

    public record StockChangeRequest(String productId, String warehouseId, int quantity) {
    }

    public record ReserveRequest(String orderId, String productId, String warehouseId, int quantity) {
    }

    public record SalesOrderRequest(
            String customerId,
            String organizationId,
            String productId,
            String warehouseId,
            int quantity
    ) {
    }

    public record ShipmentRequest(String orderId, String productId, String warehouseId, int quantity) {
    }

    public record PurchaseRequest(String supplierId, String productId, String warehouseId, int quantity) {
    }

    public record TransferRequest(String productId, String fromWarehouseId, String toWarehouseId, int quantity) {
    }

    public record PriceRequest(BigDecimal salePrice, BigDecimal discountPercent) {
    }

    public record MovementRequest(String productId, String warehouseId, int delta, String reason) {
    }

    public record OrderEventRequest(String organizationId, String orderId) {
    }

    public record StockView(String productId, String warehouseId, int quantity, int reserved, int available) {
        static StockView from(StockBalance balance) {
            return new StockView(
                    balance.getProductId(),
                    balance.getWarehouseId(),
                    balance.getQuantity(),
                    balance.getReserved(),
                    balance.available()
            );
        }
    }
}

@RestControllerAdvice
class ApiExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<String> badRequest(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(exception.getMessage());
    }

    @ExceptionHandler(InsufficientStockException.class)
    ResponseEntity<String> conflict(InsufficientStockException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(exception.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    ResponseEntity<String> missing(IllegalStateException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(exception.getMessage());
    }
}
