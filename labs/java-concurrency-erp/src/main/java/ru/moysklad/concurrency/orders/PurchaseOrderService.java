package ru.moysklad.concurrency.orders;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.moysklad.concurrency.domain.OrderStatus;
import ru.moysklad.concurrency.domain.PurchaseOrder;
import ru.moysklad.concurrency.inventory.InventoryService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
public class PurchaseOrderService {

    private final InventoryService inventory;
    private final List<PurchaseOrder> orders = Collections.synchronizedList(new ArrayList<>());

    @Autowired
    public PurchaseOrderService(InventoryService inventory) {
        this.inventory = inventory;
    }

    public PurchaseOrder receive(String supplierId, String productId, String warehouseId, int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Закупка должна быть положительной");
        }
        inventory.receive(productId, warehouseId, quantity);
        PurchaseOrder order = new PurchaseOrder(
                UUID.randomUUID().toString(),
                supplierId,
                productId,
                warehouseId,
                quantity,
                OrderStatus.RECEIVED
        );
        orders.add(order);
        return order;
    }

    public List<PurchaseOrder> orders() {
        return List.copyOf(orders);
    }
}
