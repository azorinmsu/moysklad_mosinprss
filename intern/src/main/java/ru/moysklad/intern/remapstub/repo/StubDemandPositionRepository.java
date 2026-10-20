package ru.moysklad.intern.remapstub.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.moysklad.intern.remapstub.entity.StubDemandPosition;

import java.util.List;
import java.util.UUID;

public interface StubDemandPositionRepository extends JpaRepository<StubDemandPosition, UUID> {

    List<StubDemandPosition> findByDemandIdOrderByIdAsc(UUID demandId);

    void deleteByDemandId(UUID demandId);
}
