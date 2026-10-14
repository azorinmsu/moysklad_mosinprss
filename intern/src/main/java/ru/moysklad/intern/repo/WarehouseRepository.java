package ru.moysklad.intern.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.moysklad.intern.entity.warehouse.Warehouse;

import java.util.UUID;

public interface WarehouseRepository extends JpaRepository<Warehouse, UUID> {
}
