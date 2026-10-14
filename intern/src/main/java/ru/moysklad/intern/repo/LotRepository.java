package ru.moysklad.intern.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.moysklad.intern.entity.stock.Lot;

import java.util.UUID;

public interface LotRepository extends JpaRepository<Lot, UUID> {
}
