package ru.moysklad.intern.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.moysklad.intern.entity.Modifier;

import java.util.UUID;

@Repository
public interface ModifierRepository extends JpaRepository<Modifier, UUID> {
}
