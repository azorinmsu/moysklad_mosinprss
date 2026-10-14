package ru.moysklad.intern.repo;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.moysklad.intern.entity.stock.Stock;

import java.util.List;
import java.util.UUID;

public interface StockRepository extends JpaRepository<Stock, UUID> {

    @EntityGraph(attributePaths = {"document", "place", "serialNumber", "lot"})
    List<Stock> findBySerialNumber_IdAndLot_Id(UUID serialNumberId, UUID lotId);
}
