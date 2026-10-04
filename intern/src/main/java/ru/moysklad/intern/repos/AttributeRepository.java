package ru.moysklad.intern.repos;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.moysklad.intern.entity.Attribute;

import java.util.UUID;

public interface AttributeRepository extends JpaRepository<Attribute, UUID> {
}
