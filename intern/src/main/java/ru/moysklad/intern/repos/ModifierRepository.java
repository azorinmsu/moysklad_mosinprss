package ru.moysklad.intern.repos;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.moysklad.intern.entity.Modifier;

import java.util.UUID;

public interface ModifierRepository extends JpaRepository<Modifier, UUID> {
}
