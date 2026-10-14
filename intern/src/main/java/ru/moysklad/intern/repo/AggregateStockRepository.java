package ru.moysklad.intern.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.moysklad.intern.entity.aggregate.AggregateStock;

import java.util.Optional;
import java.util.UUID;

public interface AggregateStockRepository extends JpaRepository<AggregateStock, UUID> {

    Optional<AggregateStock> findByProduct_IdAndStore_IdAndConsignment_Id(
            UUID productId,
            UUID storeId,
            UUID consignmentId
    );
}
