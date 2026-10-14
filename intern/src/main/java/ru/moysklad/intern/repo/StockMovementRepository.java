package ru.moysklad.intern.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.moysklad.intern.entity.aggregate.StockMovement;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface StockMovementRepository extends JpaRepository<StockMovement, UUID> {

    List<StockMovement> findByProduct_IdAndStore_IdAndConsignment_IdOrderByMomentAsc(
            UUID productId,
            UUID storeId,
            UUID consignmentId
    );

    /**
     * Сумма дельт движений до указанного момента включительно — для проверки исторического остатка.
     */
    @Query("""
            select coalesce(sum(m.quantityDelta), 0)
            from StockMovement m
            where m.product.id = :productId
              and m.store.id = :storeId
              and m.consignment.id = :consignmentId
              and m.moment <= :moment
            """)
    BigDecimal sumQuantityDeltaUntil(
            @Param("productId") UUID productId,
            @Param("storeId") UUID storeId,
            @Param("consignmentId") UUID consignmentId,
            @Param("moment") LocalDateTime moment
    );
}
