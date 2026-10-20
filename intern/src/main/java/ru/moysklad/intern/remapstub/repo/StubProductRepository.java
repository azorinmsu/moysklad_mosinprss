package ru.moysklad.intern.remapstub.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.moysklad.intern.remapstub.entity.StubProduct;

import java.util.UUID;

public interface StubProductRepository extends JpaRepository<StubProduct, UUID> {
}
