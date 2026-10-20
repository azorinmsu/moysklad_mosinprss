package ru.moysklad.intern.remapstub.repo;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.moysklad.intern.remapstub.entity.StubStock;

import java.util.Optional;
import java.util.UUID;

public interface StubStockRepository extends JpaRepository<StubStock, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from StubStock s where s.productId = :productId")
    Optional<StubStock> findByIdForUpdate(@Param("productId") UUID productId);
}
