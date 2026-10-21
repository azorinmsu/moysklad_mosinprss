package ru.moysklad.intern.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.moysklad.intern.entity.aggregate.Price;

import java.util.Optional;
import java.util.UUID;

public interface PriceRepository extends JpaRepository<Price, UUID> {

    Optional<Price> findByStock_Id(UUID stockId);

    boolean existsByStock_Id(UUID stockId);

    java.util.List<Price> findByProduct_Id(UUID productId);
}
