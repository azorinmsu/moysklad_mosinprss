package ru.moysklad.concurrency.orders;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.moysklad.concurrency.domain.InsufficientStockException;
import ru.moysklad.concurrency.domain.OrderStatus;
import ru.moysklad.concurrency.domain.ReservationResult;
import ru.moysklad.concurrency.domain.SalesOrder;
import ru.moysklad.concurrency.reservation.ReservationService;
import ru.moysklad.concurrency.support.SyncPoint;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SalesOrderService {

    private final ReservationService reservations;
    private final SyncPoint syncPoint;
    private final List<SalesOrder> orders = Collections.synchronizedList(new ArrayList<>());
    private final ConcurrentHashMap<String, Integer> ordersByCustomer = new ConcurrentHashMap<>();

    @Autowired
    public SalesOrderService(ReservationService reservations) {
        this(reservations, SyncPoint.NOOP);
    }

    public SalesOrderService(ReservationService reservations, SyncPoint syncPoint) {
        this.reservations = reservations;
        this.syncPoint = syncPoint;
    }

    public SalesOrder create(String customerId, String organizationId, String productId, String warehouseId, int quantity) {
        ReservationResult reservation = reservations.reserve(
                "pending-" + UUID.randomUUID(),
                productId,
                warehouseId,
                quantity
        );
        if (!reservation.accepted()) {
            throw new InsufficientStockException(productId, warehouseId, quantity, reservation.availableAtDecision());
        }
        SalesOrder order = new SalesOrder(
                UUID.randomUUID().toString(),
                customerId,
                organizationId,
                productId,
                warehouseId,
                quantity,
                OrderStatus.RESERVED
        );
        orders.add(order);
        registerOrder(customerId);
        return order;
    }

    /**
     * Учитывает заказ в счётчике покупателя.
     * Итоговое значение равно числу вызовов для этого покупателя.
     */
    public void registerOrder(String customerId) {
        Integer current = ordersByCustomer.get(customerId);
        int next = (current == null ? 0 : current) + 1;
        syncPoint.rendezvous();
        ordersByCustomer.put(customerId, next);
    }

    public int orderCount(String customerId) {
        return ordersByCustomer.getOrDefault(customerId, 0);
    }

    public List<SalesOrder> orders() {
        return List.copyOf(orders);
    }
}
