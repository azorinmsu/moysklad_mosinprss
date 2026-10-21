package ru.moysklad.intern.repo;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.moysklad.intern.entity.aggregate.AggregateStock;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AggregateStockRepository extends JpaRepository<AggregateStock, UUID> {

    Optional<AggregateStock> findByProduct_IdAndStore_IdAndConsignment_Id(
            UUID productId,
            UUID storeId,
            UUID consignmentId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select s from AggregateStock s
            where s.product.id = :productId
              and s.store.id = :storeId
              and s.consignment.id = :consignmentId
            """)
    Optional<AggregateStock> findForUpdate(
            @Param("productId") UUID productId,
            @Param("storeId") UUID storeId,
            @Param("consignmentId") UUID consignmentId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select s from AggregateStock s
            where s.product.id = :productId
              and s.store.id = :storeId
              and s.quantity > 0
            order by s.consignment.id asc
            """)
    List<AggregateStock> findAvailableForUpdate(
            @Param("productId") UUID productId,
            @Param("storeId") UUID storeId
    );

    @Query("""
            select coalesce(sum(s.quantity), 0)
            from AggregateStock s
            where s.product.id = :productId
              and s.store.id = :storeId
            """)
    BigDecimal sumQuantityByProductAndStore(
            @Param("productId") UUID productId,
            @Param("storeId") UUID storeId
    );
}
