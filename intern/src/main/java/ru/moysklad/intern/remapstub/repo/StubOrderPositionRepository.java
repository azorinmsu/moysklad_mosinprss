package ru.moysklad.intern.remapstub.repo;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.moysklad.intern.remapstub.entity.StubOrderPosition;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StubOrderPositionRepository extends JpaRepository<StubOrderPosition, UUID> {

    List<StubOrderPosition> findByOrderIdOrderByIdAsc(UUID orderId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from StubOrderPosition p where p.id = :id")
    Optional<StubOrderPosition> findByIdForUpdate(@Param("id") UUID id);
}
