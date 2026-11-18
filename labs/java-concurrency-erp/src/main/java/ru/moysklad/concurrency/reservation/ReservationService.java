package ru.moysklad.concurrency.reservation;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.moysklad.concurrency.domain.Reservation;
import ru.moysklad.concurrency.domain.ReservationResult;
import ru.moysklad.concurrency.domain.StockBalance;
import ru.moysklad.concurrency.inventory.StockLedger;
import ru.moysklad.concurrency.support.SyncPoint;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
public class ReservationService {

    private final StockLedger ledger;
    private final SyncPoint syncPoint;
    private final List<Reservation> reservations = Collections.synchronizedList(new ArrayList<>());

    @Autowired
    public ReservationService(StockLedger ledger) {
        this(ledger, SyncPoint.NOOP);
    }

    public ReservationService(StockLedger ledger, SyncPoint syncPoint) {
        this.ledger = ledger;
        this.syncPoint = syncPoint;
    }

    /**
     * Резервирует товар под заказ покупателя.
     * Успешных резервов не больше, чем доступный остаток.
     * Поле reserved совпадает с суммой количеств принятых резервов по этой позиции.
     */
    public ReservationResult reserve(String orderId, String productId, String warehouseId, int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Количество резерва должно быть положительным");
        }
        StockBalance stock = ledger.require(productId, warehouseId);
        int reserved = stock.getReserved();
        int available = stock.getQuantity() - reserved;
        syncPoint.rendezvous();
        if (available < quantity) {
            return ReservationResult.rejected(available);
        }
        stock.setReserved(reserved + quantity);
        Reservation reservation = new Reservation(
                UUID.randomUUID().toString(),
                orderId,
                productId,
                warehouseId,
                quantity
        );
        reservations.add(reservation);
        return ReservationResult.accepted(reservation);
    }

    public List<Reservation> reservations() {
        return List.copyOf(reservations);
    }
}
