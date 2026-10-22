package ru.moysklad.intern.remapstub.repo;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.moysklad.intern.remapstub.entity.StubDemand;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StubDemandRepository extends JpaRepository<StubDemand, UUID> {

    List<StubDemand> findByCustomerOrderId(UUID customerOrderId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from StubDemand d where d.id = :id")
    Optional<StubDemand> findByIdForUpdate(@Param("id") UUID id);
}
