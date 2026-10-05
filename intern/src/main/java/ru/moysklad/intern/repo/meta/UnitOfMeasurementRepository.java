package ru.moysklad.intern.repo.meta;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.moysklad.intern.entity.meta.UnitOfMeasurement;

import java.util.Optional;
import java.util.UUID;

public interface UnitOfMeasurementRepository extends JpaRepository<UnitOfMeasurement, UUID> {
    UnitOfMeasurement findByName(String name);
}
